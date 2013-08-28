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
  import global.{Literal, Constant}
  import global.{newTypeName, rootMirror, AnnotationInfo}

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
        method = defDef.symbol.asMethod
        ordinals = dfsTraverse(defDef).zipWithIndex.toMap
        accessAnnotations = for {
          access ← accesses(defDef)
          point = access.pos.point
        } yield Access(access.symbol.asTerm, point, ordinals(access))
        invokeAnnotations = for {
          apply ← invocations(defDef)
          invoked = apply.symbol.asMethod
          position = if (invoked.isConstructor) invoked.pos else apply.pos
          point = position.pointOrElse(-1)
        } yield Invoke(invoked, point, ordinals(apply))
        assignAnnotations = for {
          assign ← assignments(defDef)
          point = assign.pos.point
        } yield Assign(assign.lhs.symbol.asTerm, point, ordinals(assign))
        toAttach = accessAnnotations ::: invokeAnnotations ::: assignAnnotations
        annotationInfos = toAttach.map(toInfo)
      } yield defDef → annotationInfos).toMap

    /** @return trace of depth-first tree traversal. */
    private[this] def dfsTraverse(t: Tree): List[Tree] = t match {
      case a@ AssignTree(Select(This(_), _), _) =>
        a.children.flatMap(dfsTraverse) ::: List(a)
      case notAssignment =>
        notAssignment :: notAssignment.children.flatMap(dfsTraverse)
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
      *         - Class.super.method(...)
      *         - Mixin.$init$(...)
      *         - $this.method(...), where $this is Mixin.$init$ parameter
      */
    private[this] def invocations(t: Tree): List[Apply] = t.collect {
      case a@ Apply(Select(This(_), _), _) => a
      case a@ Apply(Select(Super(_, _), _), _) => a
      case a@ Apply(_, _) if a.symbol.isMixinConstructor => a
      case a@ Apply(Select(i: Ident, _), _)
        if i.hasSymbolWhich(_.owner.isMixinConstructor) => a
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

