// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

private[this] object ReferenceBeforeAssignmentChecker {
  trait Environment {
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
  def apply(env: Environment)
           (start: env.Instruction): Stream[List[env.Instruction]] = {
    type Stack = List[env.Instruction]
    def flattenClosure(x: Stack): Stream[Stack] = env.flatten(x.head) match {
      case Left(_) => Stream(x)
      case Right(newInstructions) => for {
        instruction ← newInstructions.sortWith(env.lessThan(_, _))
        if !x.contains(instruction)
        inflattenable ← flattenClosure(instruction :: x)
      } yield inflattenable
    }
    def instructionPairs = flattenClosure(List(start)).combinations(2).toStream
    for {
      Stream(stack@ x :: _ , y :: _) ← instructionPairs if env.conflict(x, y)
    } yield stack
  }
}

