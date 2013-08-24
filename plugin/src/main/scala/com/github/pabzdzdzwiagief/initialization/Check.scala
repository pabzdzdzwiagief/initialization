// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import reflect.internal.util.BatchSourceFile
import tools.nsc.Global
import tools.nsc.Phase
import tools.nsc.plugins.PluginComponent

import annotation._

private[this] class Check(val global: Global) extends PluginComponent {
  import global.{ClassDef, CompilationUnit, Literal, MethodSymbol}
  import ReferenceBeforeAssignmentChecker.Environment

  final val phaseName = "initcheck"

  /** Needs annotations left during `initorder` phase. */
  final val runsAfter = List("initorder")

  final def newPhase(prev: Phase): Phase = new CheckerPhase(prev)

  private class CheckerPhase(prev: Phase) extends StdPhase(prev) {
    import reflect.internal.util.OffsetPosition

    /** Warns upon detection of any reference before assignment. */
    override def apply(unit: CompilationUnit) = for {
      classDef@ ClassDef(_, _, _, _) ← unit.body
      constructor = classDef.symbol.asClass.primaryConstructor.asMethod
      checker = ReferenceBeforeAssignmentChecker(environment)(_)
      access :: tail ← checker(Invoke(constructor, constructor.pos.point))
      javaStackTrace = for {
        Invoke(method: MethodSymbol, ordinal) ← tail
        className = method.owner.fullName.toString
        methodName = method.name.toString
        fileName = method.sourceFile.name
        line = position(method, ordinal).safeLine
      } yield new StackTraceElement(className, methodName, fileName, line)
      lastMethod = tail.head.member.asInstanceOf[MethodSymbol]
      message = s"${access.member} is referenced before assignment"
      fakeException = new Exception {
        override def toString = message
        setStackTrace(javaStackTrace.toArray)
      }
    } {
      import java.io.{PrintWriter, StringWriter}
      val stringWriter = new StringWriter
      fakeException.printStackTrace(new PrintWriter(stringWriter))
      unit.warning(position(lastMethod, access.ordinal), stringWriter.toString)
    }

    private def position(method: MethodSymbol, point: Int) =
      new OffsetPosition(new BatchSourceFile(method.sourceFile), point)
  }

  private object environment extends Environment {
    type Instruction = annotation.Instruction

    def flatten(x: Instruction): Either[x.type, Stream[Instruction]] =
      x match {
        case Invoke(m: MethodSymbol, _) => Right(invoke(m).toStream)
        case _ => Left(x)
      }

    def lessThan(x: Instruction, y: Instruction) =
      x.ordinal < y.ordinal || x.ordinal == y.ordinal && (x match {
        case access: Access => false
        case invoke: Invoke => y match {
          case a: Access => true
          case _ => false
        }
        case assign: Assign => y match {
          case a: Access => true
          case i: Invoke => true
          case _ => false
        }
      })

    def conflict(x: Instruction, y: Instruction) = (x, y) match {
      case (Access(v1, _), Assign(v2, _)) => v1 == v2
      case _ => false
    }

    /** Loads information about instructions executed in given method
      * from annotations attached to it.
      */
    private def invoke(method: MethodSymbol): Seq[Instruction] = for {
      info ← method.annotations
      annotationClass = Class.forName(info.atp.typeSymbol.fullName)
      if classOf[Instruction].isAssignableFrom(annotationClass)
      args = info.args.map(_.asInstanceOf[Literal].value.value)
      anyRefArgs = args.map(_.asInstanceOf[AnyRef]).toSeq
      constructors = annotationClass.getConstructors
      init ← constructors.find(_.getParameterTypes.length == anyRefArgs.length)
    } yield init.newInstance(anyRefArgs: _*).asInstanceOf[Instruction]
  }
}


