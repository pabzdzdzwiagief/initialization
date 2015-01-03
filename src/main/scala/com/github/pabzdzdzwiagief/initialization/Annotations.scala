// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import scala.tools.nsc.Global

private[this] trait Annotations {
  val global: Global

  /** Represents something that happens during initialization procedure. */
  sealed abstract class Trace {
    /** Method from which `member` is referenced. */
    val from: global.MethodSymbol

    /** Object that identifies relevant class member. */
    val member: global.Symbol

    /** Point in relevant source file. */
    val point: Int

    /** For comparing with other annotations attached to the same symbol.
      * Instruction happens before those for which this value is greater.
      */
    val ordinal: Int
  }

  abstract sealed class Access extends Trace {
    override val member: global.TermSymbol
  }

  abstract sealed class Invocation extends Trace {
    override val member: global.MethodSymbol
  }

  case class Get(from: global.MethodSymbol, member: global.TermSymbol, point: Int, ordinal: Int)
    extends Access

  case class Set(from: global.MethodSymbol, member: global.TermSymbol, point: Int, ordinal: Int)
    extends Access

  case class Virtual(from: global.MethodSymbol, member: global.MethodSymbol, point: Int, ordinal: Int)
    extends Invocation

  case class Static(from: global.MethodSymbol, member: global.MethodSymbol, point: Int, ordinal: Int)
    extends Invocation
}
