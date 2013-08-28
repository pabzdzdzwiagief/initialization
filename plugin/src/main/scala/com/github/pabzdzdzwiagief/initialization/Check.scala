// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import java.io.{PrintWriter, StringWriter}
import reflect.internal.util.BatchSourceFile
import reflect.internal.util.OffsetPosition
import tools.nsc.Global
import tools.nsc.Phase
import tools.nsc.plugins.PluginComponent

private[this] class Check(val global: Global) extends PluginComponent {
  import global.{ClassDef, CompilationUnit, Literal, MethodSymbol, ClassSymbol}
  import ReferenceBeforeAssignmentChecker.Environment

  final val phaseName = "initcheck"

  /** Needs annotations left during `initorder` phase. */
  final val runsAfter = List("initorder")

  final def newPhase(prev: Phase): Phase = new CheckerPhase(prev)

  private[this] class CheckerPhase(prev: Phase) extends StdPhase(prev) {

    /** Warns upon detection of any reference before assignment. */
    override def apply(unit: CompilationUnit) = for {
      classDef@ ClassDef(_, _, _, _) ← unit.body
      classSymbol = classDef.symbol.asClass
      if classSymbol.primaryConstructor.exists
      constructor = classSymbol.primaryConstructor.asMethod
      start = constructor.pos.point
      checker = ReferenceBeforeAssignmentChecker(new Context(classSymbol))(_)
      _ :: accessor :: last :: tail ← checker(Invoke(constructor, start, start))
      javaStackTrace = for {
        Invoke(method: MethodSymbol, point, ordinal) ← last :: tail
        className = method.owner.fullName.toString
        methodName = method.name.toString
        fileName = method.sourceFile.name
        line = position(method, point).safeLine
      } yield new StackTraceElement(className, methodName, fileName, line)
      lastMethod = last.member.asInstanceOf[MethodSymbol]
      message = s"${accessor.member} is referenced before assignment"
      exceptionImitation = new Exception {
        override def toString = message
        setStackTrace(javaStackTrace.toArray)
      }
    } {
      val stringWriter = new StringWriter
      exceptionImitation.printStackTrace(new PrintWriter(stringWriter))
      unit.warning(position(lastMethod, accessor.point), stringWriter.toString)
    }

    private[this] def position(method: MethodSymbol, point: Int) =
      new OffsetPosition(new BatchSourceFile(method.sourceFile), point)
  }

  private[this] class Context(inClass: ClassSymbol) extends Environment {
    type Instruction = com.github.pabzdzdzwiagief.initialization.Instruction

    def flatten(x: Instruction): Either[x.type, Stream[Instruction]] =
      x match {
        case Invoke(m: MethodSymbol, _, _) => Right(invoke(m).toStream)
        case _ => Left(x)
      }

    def lessThan(x: Instruction, y: Instruction) =
      x.ordinal < y.ordinal || x.ordinal == y.ordinal && (x match {
        case assign: Assign => true
        case invoke: Invoke => y match {
          case a: Access => false
          case _ => true
        }
        case access: Access => y match {
          case a: Assign => false
          case i: Invoke => false
          case _ => true
        }
      })

    def conflict(x: Instruction, y: Instruction) = (x, y) match {
      case (Access(v1, _, _), Assign(v2, _, _)) => v1 == v2
      case _ => false
    }

    /** Loads information about instructions executed in given method
      * from annotations attached to it.
      */
    private[this] def invoke(method: MethodSymbol): Seq[Instruction] = for {
      info ← method.overridingSymbol(inClass).orElse(method).annotations
      annotationClass = Class.forName(info.atp.typeSymbol.fullName)
      if classOf[Instruction].isAssignableFrom(annotationClass)
      args = info.args.map(_.asInstanceOf[Literal].value.value)
      anyRefArgs = args.map(_.asInstanceOf[AnyRef]).toSeq
      constructors = annotationClass.getConstructors
      init ← constructors.find(_.getParameterTypes.length == anyRefArgs.length)
    } yield init.newInstance(anyRefArgs: _*).asInstanceOf[Instruction]
  }
}


