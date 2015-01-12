// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import scala.reflect.internal.Flags
import tools.nsc.Global
import tools.nsc.plugins.PluginComponent
import tools.nsc.transform.Transform

private[this] class Order(val global: Global)
  extends PluginComponent with Transform with Annotations {
  import global.{CompilationUnit, Transformer}
  import global.{Tree, ClassDef, DefDef}
  import global.{Select, This, Assign => AssignTree, Apply, Ident, Super}
  import global.{Typed, TypeTree, Annotated, AnnotatedType, AnnotationInfo}
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
          point = tree.pos.pointOrElse(-1)
        } yield new Static(from, invoked, point, ordinals(tree))
        assign = for {
          tree ← assignments(defDef) if shouldCheck(tree)
          point = tree.pos.pointOrElse(-1)
        } yield Set(from, tree.lhs.symbol.asTerm, point, ordinals(tree))
        toAttach = access ::: invoke ::: special ::: assign
        annotationInfos = toAttach.map(Trace.toAnnotation)
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

    /** @return trees that represent unchecked expressions.
      *         Matches trees of form:
      *         - (expr: @unchecked)
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
      *         - Class.this.field = ..., where this.field is immutable
      */
    private[this] def assignments(t: Tree): List[AssignTree] = t.collect {
      case a@ AssignTree(s@ Select(This(_), _), _) if !s.symbol.isMutable => a
    }

    /** @return trees that represent member method invocations.
      *         Matches trees of form:
      *         - Class.this.method(...)
      *         - $this.method(...), where $this is Mixin.$init$ parameter
      *         - Trait$class.method(this, ...), where Trait$class is Trait's
      *                                          implementation module
      *         - $outer.$outer1.$outer2.method(...), where $outer is an outer
      *                                               parameter used in
      *                                               a constructor of an
      *                                               inner class and $outerN
      *                                               is an outer accessor
      *         - this.$outer1.$outer2.method(...), where $outerN is an outer
      *                                             accessor used in a method
      *                                             of an inner class
      *         - this.x.y.z(...), where any of x, y, z is a member
      *                            of an inner class
      *
      */
    private[this] def invocations(t: DefDef): List[Apply] = t.collect {
      case a@ Apply(Select(This(_), _), _) => a
      case a@ Apply(Select(i: Ident, _), _)
        if i.hasSymbolWhich(_.name == global.nme.SELF)
        && i.hasSymbolWhich(_.owner.owner.isTrait) => a
      case a@ Apply(_: Select, This(_) :: _)
        if t.hasSymbolWhich(_.hasFlag(Flags.MIXEDIN))
        && a.hasSymbolWhich(_.isMethod)
        && a.hasSymbolWhich(_.owner.isImplClass) => a
      case a@ Apply(Select(outer, _), _)
        if a.hasSymbolWhich(!_.isOuterAccessor)
        && outer.forAll {
          case i@ Ident(global.nme.OUTER)
            if i.hasSymbolWhich(_.isValueParameter)
            && i.hasSymbolWhich(_.owner.isConstructor)
            && i.hasSymbolWhich(_.owner.owner.isLifted) => true
          case This(_) => true
          case accessorChainSelect: Select
            if accessorChainSelect.hasSymbolWhich(_.isOuterAccessor) => true
          case accessorChainApply: Apply
            if accessorChainApply.hasSymbolWhich(_.isOuterAccessor) => true
          case _ => false
        } => a
      case a: Apply
        if a.exists { case Select(This(_), _) => true case _ => false }
        && a.hasSymbolWhich(_.owner.isLifted) => a
    }

     /** @return trees that represent special member method invocations.
       *         Matches trees of form:
       *        - (new anonymous class anonfun$1(Class.this) : FunctionN),
       *          where anonfun$1 is an anonymous function, the returned tree
       *          is however not a reference to constructor, but to apply()
       *          method
       *        - Class.super.method(...)
       *        - Mixin.$init$(...)
       *        - new Class.Inner(...), where Inner is an inner class
       *                                enclosed by Class
       */
    private[this] def specials(t: Tree): List[Apply] = t.collect {
      case Typed(a@ Apply(_, This(_) :: _), functionType: TypeTree)
        if a.hasSymbolWhich(_.isConstructor)
        && a.hasSymbolWhich(_.owner.isAnonymousFunction) =>
        new Apply(a.fun, a.args) {
          override var symbol = a.symbol.owner.info.findMember(
            global.nme.apply,
            excludedFlags = Flags.BRIDGE | Flags.DEFERRED,
            requiredFlags = Flags.FINAL | Flags.METHOD,
            stableOnly = false
          ).alternatives.headOption.getOrElse(a.symbol)
          setPos(a.pos)
          override def hashCode() = a.hashCode()
          override def equals(b: Any) = a.equals(b)
      }
      case a@ Apply(Select(Super(_, _), _), _) => a
      case a@ Apply(_, _) if a.symbol.isMixinConstructor => a
      case a@ Apply(_, This(_) :: _)
        if a.hasSymbolWhich(_.isConstructor)
        && a.hasSymbolWhich(_.owner.isLifted) => a
    }

    /** @return trees that represent member accesses.
      *         Matches trees of form:
      *         - Class.this.field, inside stable member accessor def
      *         - Class.this, where Class is an outer class
      */
    private[this] def accesses(t: DefDef): List[Select] = t match {
      case d if d.symbol.isAccessor && d.symbol.isStable => d.collect {
        case s@ Select(This(_), _) if s.symbol.isPrivateLocal => s
      }
      case d if d.symbol.isOuterAccessor => d.collect {
        case s@ Select(This(_), _) if s.symbol.isProtected => s
      }
      case _ => Nil
    }
  }
}
