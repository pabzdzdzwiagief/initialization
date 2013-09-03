// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import java.io.{PrintWriter, StringWriter}

private[this] object ErrorPresenter {
  trait Environment {
    /** Error message template. */
    case class Error(location: Option[Location], message: String)

    /** Program instruction, like variable access or method invocation. */
    type Instruction

    /** Location in a source file. */
    type Location

    /** @return name of class to which belonds member used by instruction. */
    def className(x: Instruction): String

    /** @return name of method called by instruction. */
    def methodName(x: Instruction): String

    /** @param x executed instruction.
      * @param context member in context of which the accessor is invoked.
      * @return some location of errorneous reference, none if it is unknown.
      */
    def location(x: Instruction, context: Instruction): Option[Location]

    /** @return name of file. */
    def fileName(x: Location): String

    /** @return line number. */
    def line(x: Location): Int
  }

  /** @param env compiler environment.
    * @param stackTrace begins with the deepest ("most recent") instruction.
    * @return some template for an error message on success, none if the stack
    *         trace is too short to say anything.
    */
  def apply(env: Environment)
           (stackTrace: List[env.Instruction]): Option[env.Error] = for {
    _ :: accessor :: last :: tail ← Some(stackTrace)
    javaStackTrace = for {
      (instruction, context) ← (last :: tail).zip(tail :+ stackTrace.last)
      location = env.location(instruction, context)
      className = env.className(instruction)
      methodName = env.methodName(instruction)
      fileName = location.map(env.fileName(_)).getOrElse(null)
      line = location.map(env.line(_)).getOrElse(-1)
    } yield new StackTraceElement(className, methodName, fileName, line)
    exceptionImitation = exception(env.methodName(accessor))(javaStackTrace)
    location = env.location(accessor, last)
    message = printToString(exceptionImitation)
  } yield env.Error(location, message)

  /** @param name name of member that can be referenced before assignment.
    * @param trace list of stack trace elements beginning with the deepest one.
    * @return fake exception that immitates one that could be thrown upon
    *         access to uninitialized value.
    */
  private[this] def exception(name: String)
                             (trace: List[StackTraceElement]) = new Exception {
    override def toString = s"value $name is referenced before assignment"
    setStackTrace(trace.toArray)
  }

  /** @return exception's stack trace printed to string. */
  private[this] def printToString(exception: Exception) = {
    val stringWriter = new StringWriter
    exception.printStackTrace(new PrintWriter(stringWriter))
    stringWriter.toString
  }
}
