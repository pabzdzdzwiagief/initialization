// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import scala.tools.nsc.Global

import com.github.pabzdzdzwiagief.initialization.{Trace => JavaTrace}

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

  object Trace {
    import global.{LiteralAnnotArg, Constant, AnnotationInfo}
    import global.rootMirror.getRequiredClass
    import global.nme.{CONSTRUCTOR, getterToLocal}
    import global.stringToTermName

    /** Converts Scalac's internal annotation representation to a Trace
      * object if the annotation represents a Trace.
      */
    def fromAnnotation(anyAnnotation: AnnotationInfo,
                       from: global.MethodSymbol,
                       inClass: global.ClassSymbol): Option[Trace] = for {
      info ← Some(anyAnnotation)
      if info.atp <:< traceType
      map = info.javaArgs collect {
        case (n, a: LiteralAnnotArg) => (n.decoded, a.const)
      }
      Constant(owner: String) = map("owner")
      Constant(memberName: String) = map("memberName")
      Constant(fromMemberName: String) = map("fromMemberName")
      Constant(typeString: String) = map("typeString")
      Constant(fromTypeString: String) = map("fromTypeString")
      Constant(traceType: String) = map("traceType")
      Constant(point: Int) = map("point")
      Constant(ordinal: Int) = map("ordinal")
      fromType = getRequiredClass(owner).tpe
      if fromMemberName == from.nameString
      if fromTypeString == from.info.safeToString
      toNameRaw = global.stringToTermName(memberName)
      toName = if (toNameRaw == CONSTRUCTOR) toNameRaw else toNameRaw.encode
      (name, mkTrace) = traceType match {
        case "Static" => (toName, { s: global.Symbol =>
          Static(from, s.asMethod, point, ordinal)
        })
        case "Virtual" => (toName, { s: global.Symbol  =>
          Virtual(from, s.overridingSymbol(inClass)
                         .orElse(s)
                         .asMethod, point, ordinal)
        })
        case "Get" => (getterToLocal(toName), { s: global.Symbol =>
          Get(from, s.asTerm, point, ordinal)
        })
        case "Set" => (getterToLocal(toName), { s: global.Symbol =>
          Set(from, s.asTerm, point, ordinal)
        })
      }
      symbol ← fromType.memberBasedOnName(name, 0).alternatives.find {
        fromType.memberType(_).safeToString == typeString
      }
    } yield mkTrace(symbol)

    /** Converts Trace object to Scalac's internal annotation representation. */
    def toAnnotation(trace: Trace): AnnotationInfo = {
      val name = classOf[JavaTrace].getCanonicalName
      def a(x: Any) = LiteralAnnotArg(Constant(x))
      def n(s: String) = stringToTermName(s)
      AnnotationInfo(getRequiredClass(name).tpe, Nil, List(
        n("owner") → a(trace.member.owner.fullNameString),
        n("memberName") → a(trace.member.nameString),
        n("fromMemberName") → a(trace.from.nameString),
        n("fromTypeString") → a(trace.from.info.safeToString),
        n("typeString") → a(trace.member.info.safeToString),
        n("traceType") → a(trace.getClass.getSimpleName),
        n("point") → a(trace.point),
        n("ordinal") → a(trace.ordinal)
      ))
    }

    private[this] val traceType =
      getRequiredClass(classOf[JavaTrace].getCanonicalName).tpe
  }
}
