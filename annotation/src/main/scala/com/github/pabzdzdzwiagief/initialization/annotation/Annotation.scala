// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization.annotation

import annotation.StaticAnnotation

/** Annotation left by the Initialization plugin. */
sealed abstract class Annotation extends StaticAnnotation

/** Represents something that happens during initialization procedure. */
sealed abstract class Instruction extends Annotation with Product {
  /** Object that identifies relevant class member. */
  def member: AnyRef

  /** Point in relevant source file. */
  def point: Int

  /** For comparing with other annotations attached to the same symbol.
    * Instruction happens before those for which this value is greater.
    */
  def ordinal: Int
}
final case class Access(member: AnyRef, point: Int, ordinal: Int)
  extends Instruction
final case class Assign(member: AnyRef, point: Int, ordinal: Int)
  extends Instruction
final case class Invoke(member: AnyRef, point: Int, ordinal: Int)
  extends Instruction
