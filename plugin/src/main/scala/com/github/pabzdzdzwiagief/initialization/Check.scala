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
      startingState = State(Invoke(constructor, constructor.pos.point), Nil)
      instructionPairs = simulateExecution(startingState).combinations(2)
      Stream(State(Access(x, _), s), State(Assign(y, _), _)) ← instructionPairs
      if x == y
      stackTrace = for {
        trace ← s.tail
        className = trace.method.owner.fullName.toString
        methodName = trace.method.name.toString
        fileName = trace.method.sourceFile.name
        line = trace.position.safeLine
      } yield new StackTraceElement(className, methodName, fileName, line)
      fakeException = new Exception {
        override def toString = s"$x is referenced before assignment"
        setStackTrace(stackTrace.toArray)
      }
    } {
      import java.io.{PrintWriter, StringWriter}
      val stringWriter = new StringWriter
      fakeException.printStackTrace(new PrintWriter(stringWriter))
      unit.warning(s.head.position, stringWriter.toString)
    }

    /** State of execution, i.e. instruction and stack trace. */
    private case class State(current: Instruction, stack: List[Trace])

    /** Stack trace element. */
    private case class Trace(method: MethodSymbol, offset: Int) {
      val position =
        new OffsetPosition(new BatchSourceFile(method.sourceFile), offset)
    }

    /** Generates a sequence of primitive instructions (accesses or assignments)
      * possibly executed after starting from given state (i.e. simulates
      * method invocations).
      */
    private def simulateExecution(from: State): Stream[State] = from match {
      case State(invoked@ Invoke(method: MethodSymbol, offset), stack) =>
        if (stack.contains(method)) Stream.empty
        else for {
          complex ← instructions(method).sortBy(_.ordinal).toStream
          lastTrace = Trace(method, offset)
          primitive ← simulateExecution(State(complex, lastTrace :: from.stack))
        } yield primitive
      case state@ State(current, stack@ Trace(method, _) :: _) =>
        Stream(state.copy(stack = Trace(method, current.ordinal) :: stack))
    }

    /** Loads information about instructions executed in given method
      * from annotations attached to it.
      */
    private def instructions(method: MethodSymbol): Seq[Instruction] = for {
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

private object referenceBeforeAssignmentChecker {
  trait Compiler {
    /** Program instruction, like variable access or method invocation. */
    type Instruction <: AnyRef

    /** @return Left if x is inflattenable, Right if given instruction can be
      *         broken down into simpler instructions.
      */
    def flatten(x: Instruction): Either[x.type, Stream[Instruction]]

    /** @return true if x happens before y, false otherwise. */
    def lessThan(x: Instruction, y: Instruction): Boolean

    /** @return true if executing x before y should be considered errorneous,
      *         false otherwise.
      */
    def conflict(x: Instruction, y: Instruction): Boolean
  }

  /** @param env compiler environment.
    * @param start instruction stating from which execution is simulated
    *              in search of reference-before-assignment errors.
    * @return stream of stack traces to values which are referenced
    *         before their assignment.
    */
  def apply(env: Compiler)
           (start: env.Instruction): Stream[List[env.Instruction]] = {
    type Stack = List[env.Instruction]
    def flattenClosure(x: Stack): Stream[Stack] = env.flatten(x.head) match {
      case Left(_) => Stream(x)
      case Right(newInstructions) => for {
        instruction ← newInstructions.sortWith(env.lessThan(_, _))
        inflattenable ← flattenClosure(instruction :: x)
      } yield inflattenable
    }
    def instructionPairs = flattenClosure(List(start)).combinations(2).toStream
    for {
      Stream(stack@ x :: _ , y :: _) ← instructionPairs if env.conflict(x, y)
    } yield stack
  }
}
