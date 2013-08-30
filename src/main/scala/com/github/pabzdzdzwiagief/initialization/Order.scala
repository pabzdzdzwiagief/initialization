// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import tools.nsc.Global
import tools.nsc.plugins.PluginComponent
import tools.nsc.transform.Transform

private[this] class Order(val global: Global)
  extends PluginComponent with Transform {
  import global.{CompilationUnit, Transformer}
  import global.{Tree, ClassDef, DefDef}
  import global.{Select, This, Assign => AssignTree, Apply, Ident, Super}
  import global.{Typed, TypeTree, Annotated, AnnotatedType}
  import global.{Literal, Constant}
  import global.{newTypeName, rootMirror, AnnotationInfo}
  import global.definitions.UncheckedClass.{tpe => uncheckedType}

  final val phaseName = "initorder"

  /** Runs after AST becomes as simple as it can get. */
  final val runsAfter = List("cleanup")

  final def newTransformer(unit: CompilationUnit): Transformer = annotator

  private[this] val annotator: Transformer = new Transformer {
    /** Annotates methods of every class.
      * Annotations inform about anything that can help spotting possible
      * initialization problems, e.g. which class members are used.
      */
    override def transform(tree: Tree): Tree = super.transform(tree) match {
      case classDef: ClassDef => {
        for {
          (defDef, toAttach) ← infos(classDef)
          method = defDef.symbol.asMethod
          annotationInfo ← toAttach
        } {
          method.addAnnotation(annotationInfo)
        }
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
        ordinals = dfsTraverse(defDef).zipWithIndex.toMap
        shouldCheck = (for {
          Typed(expression, _) ← unchecks(defDef)
          child ← expression :: expression.children
        } yield child).toSet.andThen(!_)
        access = for {
          tree ← accesses(defDef) if shouldCheck(tree)
          point = tree.pos.point
        } yield Access(tree.symbol.asTerm, point, ordinals(tree))
        invoke = for {
          tree ← invocations(defDef) if shouldCheck(tree)
          invoked = tree.symbol.asMethod
          point = tree.pos.point
        } yield Invoke(invoked, point, ordinals(tree))
        special = for {
          tree ← specials(defDef) if shouldCheck(tree)
          invoked = tree.symbol.asMethod
          position = if (invoked.isConstructor) invoked.pos else tree.pos
          point = position.pointOrElse(-1)
        } yield new Special(invoked, point, ordinals(tree))
        assign = for {
          tree ← assignments(defDef) if shouldCheck(tree)
          point = tree.pos.point
        } yield Assign(tree.lhs.symbol.asTerm, point, ordinals(tree))
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
    private[this] def toInfo(annotation: Instruction): AnnotationInfo = {
      val name = newTypeName(annotation.getClass.getCanonicalName)
      val classSymbol = rootMirror.getClassByName(name)
      val args = annotation.productIterator.map(c => Literal(Constant(c)))
      AnnotationInfo(classSymbol.tpe, args.toList, Nil)
    }
  }
}

