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
  import global.{rootMirror, newTypeName, AnnotationInfo}

  final val phaseName = "initorder"

  /** Runs after AST becomes as simple as it can get. */
  final val runsAfter = List("cleanup")

  final def newTransformer(unit: CompilationUnit): Transformer = annotator

  private[this] val annotator: Transformer = new Transformer {
    /** Annotates methods of every class.
      * Annotations inform about anything that can help spotting possible
      * initialization problems, e.g. which class members are used.
      */
    override def transform(tree: Tree): Tree = {
      tree match {
        case classDef: ClassDef => annotate(classDef)
        case _ =>
      }
      super.transform(tree)
    }

    /** Annotates methods with information about what happens in them. */
    private[this] def annotate(classDef: ClassDef) = for {
      method@ DefDef(_, _, _,  _, _, _) ← classDef.impl.body
      m = method.symbol.asMethod
      subtree ← method
    } subtree match {
      case AssignTree(s@ Select(This(_), _), _) =>
        m.addAnnotation(info(Assign(s.symbol.asTerm, s.pos.point, s.pos.point)))
      case Apply(s@ Select(This(_), _), _) if s.symbol.ne(null) => {
        val instruction = if (s.symbol.isAccessor && s.symbol.isStable) {
          Access(s.symbol.accessed.asTerm, s.pos.point, s.pos.point)
        } else {
          Invoke(s.symbol.asMethod, s.pos.point, s.pos.point)
        }
        m.addAnnotation(info(instruction))
      }
      case _ =>
    }

    /** Converts regular annotation object to
      * [[scala.reflect.internal.AnnotationInfos#AnnotationInfo]].
      */
    private[this] def info(annotation: Instruction): AnnotationInfo = {
      val name = newTypeName(annotation.getClass.getCanonicalName)
      val classSymbol = rootMirror.getClassByName(name)
      val args = annotation.productIterator.map(c => Literal(Constant(c)))
      AnnotationInfo(classSymbol.tpe, args.toList, Nil)
    }
  }
}
