// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import scala.tools.nsc.Global

private[this] trait Annotations {
  val global: Global

  /** Represents something that happens during initialization procedure. */
  sealed abstract class Trace {
    /** Object that identifies relevant class member. */
    val member: global.Symbol

    /** Point in relevant source file. */
    val point: Int

    /** For comparing with other annotations attached to the same symbol.
      * Instruction happens before those for which this value is greater.
      */
    val ordinal: Int
  }

  final case class Access(member: global.TermSymbol, point: Int, ordinal: Int)
    extends Trace

  final case class Assign(member: global.TermSymbol, point: Int, ordinal: Int)
    extends Trace

  sealed case class Invoke(member: global.MethodSymbol, point: Int, ordinal: Int)
    extends Trace

  final class Special(member: global.MethodSymbol, point: Int, ordinal: Int)
    extends Invoke(member, point, ordinal)
}
