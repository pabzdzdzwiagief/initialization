// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import reflect.internal.util.BatchSourceFile
import reflect.internal.util.{NoPosition, OffsetPosition}
import tools.nsc.Global
import tools.nsc.Phase
import tools.nsc.plugins.PluginComponent

import com.github.pabzdzdzwiagief.initialization.{Trace => TraceAnnotation}

private[this] class Check(val global: Global) extends PluginComponent with Annotations {
  import global.{ClassDef, CompilationUnit, LiteralAnnotArg, Constant}
  import global.{MethodSymbol, ClassSymbol}
  import global.rootMirror.getRequiredClass
  import global.nme.{getterToLocal, CONSTRUCTOR}
  import ReferenceBeforeAssignmentChecker.Environment

  override final val phaseName = "initcheck"

  /** Needs annotations left during `initorder` phase. */
  override final val runsAfter = List("initorder")

  override final def newPhase(prev: Phase) = new StdPhase(prev) {
    /** Warns upon detection of any reference before assignment. */
    override def apply(unit: CompilationUnit) = try {
      for {
        classDef@ ClassDef(_, _, _, _) ← unit.body
        classSymbol = classDef.symbol.asClass
        if classSymbol.primaryConstructor.exists
        checker = ReferenceBeforeAssignmentChecker(new Context(classSymbol))(_)
        present = ErrorFormatter(formatterEnvironment)(_)
        constructor = classSymbol.primaryConstructor.asMethod
        start = constructor.pos.pointOrElse(-1)
        stackTrace ← checker(Invoke(constructor, start, start))
        error = present(stackTrace).orElse(throw badStackException)
        formatterEnvironment.Error(where, message) ← error
      } {
        unit.warning(where.getOrElse(NoPosition), message)
      }
    } catch {
      case e: Exception =>
        unit.warning(NoPosition, s"$phaseName: failed with exception: $e")
    }

    private[this] object badStackException extends Exception
  }

  private[this] class Context(inClass: ClassSymbol) extends Environment {
    type Instruction = Trace

    def flatten(x: Instruction): Either[x.type, Stream[Instruction]] = x match {
      case Invoke(m: MethodSymbol, _, _) => Right(follow(m).toStream)
      case _ => Left(x)
    }

    def lessThan(x: Instruction, y: Instruction) = x.ordinal < y.ordinal

    def conflict(x: Instruction, y: Instruction) = (x, y) match {
      case (Access(v1, _, _), Assign(v2, _, _)) => v1 == v2
      case _ => false
    }

    /** Loads information about instructions executed in given method
      * from annotations attached to it.
      */
    private[this] def follow(method: MethodSymbol): Seq[Trace] = for {
      info ← method.annotations
      if info.atp <:< traceType
      map = info.javaArgs map {
        case (n, a: LiteralAnnotArg) => (n.decoded, a.const)
      }
      Constant(owner: String) = map("owner")
      Constant(memberName: String) = map("memberName")
      Constant(typeString: String) = map("typeString")
      Constant(traceType: String) = map("traceType")
      Constant(point: Int) = map("point")
      Constant(ordinal: Int) = map("ordinal")
      fromType = getRequiredClass(owner).tpe
      rawName = global.stringToTermName(memberName)
      internalName = if (rawName == CONSTRUCTOR) rawName else rawName.encode
      name = traceType match {
        case "Special" | "Invoke"  => internalName
        case "Access" | "Assign" => getterToLocal(internalName)
      }
      symbol ← fromType.memberBasedOnName(name, 0).alternatives
      if fromType.memberType(symbol).safeToString == typeString
    } yield traceType match {
      case "Special" => new Special(symbol.asMethod, point, ordinal)
      case "Invoke" => Invoke(symbol.overridingSymbol(inClass)
                                    .orElse(symbol)
                                    .asMethod, point, ordinal)
      case "Access" => Access(symbol.asTerm, point, ordinal)
      case "Assign" => Assign(symbol.asTerm, point, ordinal)
    }

    private[this] val traceType =
      getRequiredClass(classOf[TraceAnnotation].getCanonicalName).tpe
  }

  private[this] object formatterEnvironment extends ErrorFormatter.Environment {
    type Instruction = Trace

    type Location = OffsetPosition

    def className(x: Instruction): String = x.member.owner.fullName.toString

    def methodName(x: Instruction): String = x.member.name.toString

    def location(x: Instruction, context: Instruction): Option[Location] = for {
      source ← Option(context.member.sourceFile)
      point ← if (x.point == -1) None else Some(x.point)
    } yield new OffsetPosition(new BatchSourceFile(source), point)

    def fileName(x: Location): String = x.source.file.name

    def line(x: Location): Int = x.safeLine
  }


}
