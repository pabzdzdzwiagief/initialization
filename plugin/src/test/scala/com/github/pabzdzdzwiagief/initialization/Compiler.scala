// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import tools.nsc.Global
import tools.nsc.GenericRunnerSettings
import tools.nsc.plugins.Plugin
import tools.nsc.reporters.ConsoleReporter
import reflect.internal.util.SourceFile
import java.io.{StringWriter, PrintWriter}

/** Compiles Scala source code.
  * @param pluginClasses classes of nsc plugins to use during compilation.
  */
class Compiler(pluginClasses: Plugin.AnyClass*) extends (SourceFile => String) {
  /** Performs compilation. Produces resulting classfiles as side-effects.
    * @param source source file to compile.
    * @return messages issued by compiler (compilation errors, warnings etc.).
    */
  def apply(source: SourceFile): String = {
    def removeLastNewline(string: String) =
      if (string.lastOption.exists(_ == '\n')) string.init else string
    def tabsToEightSpaces(string: String) =
      string.replace("\t", "        ")
    val compilation = new global.Run
    compilation.compileSources(List(source))
    val output = writer.getBuffer.toString
    writer.getBuffer.setLength(0)
    tabsToEightSpaces(removeLastNewline(output))
  }

  /** Output buffer for compiler reports. */
  private val writer = new StringWriter

  /** nsc compiler object with additional plugins. */
  private val global = {
    val settings = {
      import annotation.{Annotation => ForAnnotations}
      import runtime.{Boxed => ForScalaLibrary}
      import Class.forName
      def path(name: String) =
        forName(name).getProtectionDomain.getCodeSource.getLocation.getPath
      val value = new GenericRunnerSettings(e => throw new RuntimeException(e))
      value.classpath.append(path(classOf[Global].getName))
      value.classpath.append(path(classOf[ForAnnotations].getName))
      value.bootclasspath.append(path(classOf[ForScalaLibrary].getName))
      value.bootclasspath.append(path(classOf[ForAnnotations].getName))
      value
    }
    val reporter = new ConsoleReporter(settings, null, new PrintWriter(writer))
    new Global(settings, reporter) {
      /** Adds plugins to those loaded from jars, classfiles etc. */
      override protected def loadRoughPluginsList: List[Plugin] =
        super.loadRoughPluginsList ::: pluginClasses.toList.map {
          Plugin.instantiate(_, this)
        }
    }
  }
}
