// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import tools.nsc.Global
import tools.nsc.plugins.PluginComponent
import tools.nsc.transform.Transform

import annotation._

private[this] class Order(val global: Global)
  extends PluginComponent with Transform {
  import global.{CompilationUnit, Transformer}
  import global.{Tree, ClassDef, DefDef}
  import global.{Select, This, Assign => AssignTree, Apply, Literal, Constant}
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
        assign = assignments(defDef)
        invoke = invocations(defDef)
        assignmentContext: Map[Apply, Set[AssignTree]] =
          assign.view
                .flatMap(a => invocations(a).map(i => (i, a)))
                .groupBy(_._1)
                .mapValues(_.unzip._2.toSet)
                .withDefaultValue(Set.empty)
        assignAnnotations = for {
          assignTree ← assign
          point = assignTree.pos.point
        } yield Assign(assignTree.lhs.symbol.asTerm, point, point)
        invokeAnnotations = for {
          apply ← invoke
          context = assignmentContext(apply)
          invoked = apply.symbol.asMethod
          isAccess = invoked.isAccessor && invoked.isStable
          annotation = if (isAccess) Access else Invoke
          symbol = if (isAccess) invoked.accessed.asTerm else invoked
          point = apply.pos.point
          ordinal = (apply :: context.toList).minBy(_.pos.point).pos.point
        } yield annotation(symbol, point, ordinal)
        annotations = (assignAnnotations ::: invokeAnnotations).map(toInfo)
      } yield defDef → annotations).toMap

    /** @return trees that represent member assignments. */
    private[this] def assignments(t: Tree): List[AssignTree] = t.collect {
      case a@ AssignTree(Select(This(_), _), _) => a
    }

    /** @return trees that represent member method invocations. */
    private[this] def invocations(t: Tree): List[Apply] = t.collect {
      case a@ Apply(Select(This(_), _), _) if a.symbol.ne(null) => a
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
