// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import tools.nsc.Global
import tools.nsc.plugins.PluginComponent
import tools.nsc.transform.Transform

import com.github.pabzdzdzwiagief.initialization.{Trace => TraceAnnotation}

private[this] class Order(val global: Global)
  extends PluginComponent with Transform with Annotations {
  import global.{CompilationUnit, Transformer}
  import global.{Tree, ClassDef, DefDef}
  import global.{Select, This, Assign => AssignTree, Apply, Ident, Super}
  import global.{Typed, TypeTree, Annotated, AnnotatedType}
  import global.{LiteralAnnotArg, Constant}
  import global.AnnotationInfo
  import global.rootMirror.getRequiredClass
  import global.stringToTermName
  import global.definitions.UncheckedClass.{tpe => uncheckedType}

  override final val phaseName = "initorder"

  /** Runs after AST becomes as simple as it can get. */
  override final val runsAfter = List("cleanup")

  override final def newTransformer(unit: CompilationUnit) = new Transformer {
    /** Annotates methods of every class.
      * Annotations inform about anything that can help spotting possible
      * initialization problems, e.g. which class members are used.
      */
    override def transform(tree: Tree): Tree = super.transform(tree) match {
      case classDef: ClassDef => try {
        for {
          (defDef, toAttach) ← infos(classDef)
          method ← defDef.symbol.alternatives if method.isMethod
          annotationInfo ← toAttach
        } {
          classDef.symbol.addAnnotation(annotationInfo)
        }
        classDef
      } catch {
        case e: Exception =>
          unit.warning(classDef.pos, s"$phaseName: failed with exception: $e")
          classDef
      }
      case other => other
    }

    /** @return a map from method definitions to annotations that should be
      *         attached to them.
      */
    private[this] def infos(c: ClassDef): Map[DefDef, List[AnnotationInfo]] =
      (for {
        defDef@ DefDef(_, _, _,  _, _, _) ←  c.impl.body
        from = defDef.symbol.asMethod
        ordinals = dfsTraverse(defDef).zipWithIndex.toMap
        shouldCheck = (for {
          Typed(expression, _) ← unchecks(defDef)
          child ← expression :: expression.children
        } yield child).toSet.andThen(!_)
        access = for {
          tree ← accesses(defDef) if shouldCheck(tree)
          point = tree.pos.pointOrElse(-1)
        } yield Get(from, tree.symbol.asTerm, point, ordinals(tree))
        invoke = for {
          tree ← invocations(defDef) if shouldCheck(tree)
          invoked = tree.symbol.asMethod
          point = tree.pos.pointOrElse(-1)
        } yield Virtual(from, invoked, point, ordinals(tree))
        special = for {
          tree ← specials(defDef) if shouldCheck(tree)
          invoked = tree.symbol.asMethod
          position = if (invoked.isConstructor) invoked.pos else tree.pos
          point = position.pointOrElse(-1)
        } yield new Static(from, invoked, point, ordinals(tree))
        assign = for {
          tree ← assignments(defDef) if shouldCheck(tree)
          point = tree.pos.pointOrElse(-1)
        } yield Set(from, tree.lhs.symbol.asTerm, point, ordinals(tree))
        toAttach = access ::: invoke ::: special ::: assign
        annotationInfos = toAttach.map(toInfo)
      } yield defDef → annotationInfos).toMap

    /** Works like [[scala.reflect.internal.Trees#Tree.children]], but puts
      * assignments after their subtrees.
      *
      * @return trace of depth-first tree traversal.
      */
    private[this] def dfsTraverse(t: Any): List[Tree] = t match {
      case a@ AssignTree(Select(This(_), _), _) =>
        a.productIterator.toList.flatMap(dfsTraverse) ::: List(a)
      case tree: Tree =>
        tree :: tree.productIterator.toList.flatMap(dfsTraverse)
      case list: List[_] => list.flatMap(dfsTraverse)
      case _ => Nil
    }

    /** @return trees that represent member accesses.
      *         Matches trees of form:
      *         - (expr: @uncheckedInitialization)
      */
    private[this] def unchecks(t: DefDef): List[Typed] = t.collect {
      case t@ Typed(_, tpt: TypeTree) if (tpt.original match {
        case a: Annotated => a.tpe match {
          case AnnotatedType(i, _, _) => i.exists(_.tpe <:< uncheckedType)
          case _ => false
        }
        case _ => false
      }) => t
    }

    /** @return trees that represent member assignments.
      *         Matches trees of form:
      *         - Class.this.field = ...
      */
    private[this] def assignments(t: Tree): List[AssignTree] = t.collect {
      case a@ AssignTree(s@ Select(This(_), _), _) if !s.symbol.isMutable => a
    }

    /** @return trees that represent member method invocations.
      *         Matches trees of form:
      *         - Class.this.method(...)
      *         - $this.method(...), where $this is Mixin.$init$ parameter
      */
    private[this] def invocations(t: Tree): List[Apply] = t.collect {
      case a@ Apply(Select(This(_), _), _) => a
      case a@ Apply(Select(i: Ident, _), _)
        if i.hasSymbolWhich(_.owner.isMixinConstructor) => a
    }

     /** @return trees that represent special member method invocations.
       *         Matches trees of form:
       *        - Class.super.method(...)
       *        - Mixin.$init$(...)
       */
    private[this] def specials(t: Tree): List[Apply] = t.collect {
      case a@ Apply(Select(Super(_, _), _), _) => a
      case a@ Apply(_, _) if a.symbol.isMixinConstructor => a
    }

    /** @return trees that represent member accesses.
      *         Matches trees of form:
      *         - Class.this.field, inside stable member accessor def
      */
    private[this] def accesses(t: DefDef): List[Select] = t match {
      case d if d.symbol.isAccessor && d.symbol.isStable => d.collect {
        case s@ Select(This(_), _) if s.symbol.isPrivateLocal => s
      }
      case _ => Nil
    }

    /** Converts regular annotation object to
      * [[scala.reflect.internal.AnnotationInfos#AnnotationInfo]].
      */
    private[this] def toInfo(annotation: Trace): AnnotationInfo = {
      val name = classOf[TraceAnnotation].getCanonicalName
      def a(x: Any) = LiteralAnnotArg(Constant(x))
      def n(s: String) = stringToTermName(s)
      AnnotationInfo(getRequiredClass(name).tpe, Nil, List(
        n("owner") → a(annotation.member.owner.fullNameString),
        n("memberName") → a(annotation.member.nameString),
        n("fromMemberName") → a(annotation.from.nameString),
        n("fromTypeString") → a(annotation.from.info.safeToString),
        n("typeString") → a(annotation.member.info.safeToString),
        n("traceType") → a(annotation.getClass.getSimpleName),
        n("point") → a(annotation.point),
        n("ordinal") → a(annotation.ordinal)
      ))
    }
  }
}
