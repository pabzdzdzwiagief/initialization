// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import io.Source
import tools.nsc.io.File
import java.io.{File => JFile}

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter

/** Tests if compilation outputs are matching those expected. */
class InitializationCheckingSuite extends FunSuite with BeforeAndAfter {
  for (fileName <- testFileNames) {
    test(s"${readTitle(fileName)} in $fileName") {
      expectResult(readExpectedOutput(fileName)) {
        compile(source(fileName))
      }
    }
  }

  before {
    if (outputDirectory.exists) {
      throw new RuntimeException("""Output directory for test compilation does
                                   |already exist. Performing the test may
                                   |result in loss of its content.
                                 """.stripMargin)
    }
  }

  after {
    outputDirectory.deleteRecursively()
  }

  /** Compilation output directory for example source compiled during tests. */
  lazy val outputDirectory = new File(new JFile("localhost"))

  /** Resource files used for testing. */
  lazy val testFileNames = {
    val source = Source.fromInputStream(getClass.getResourceAsStream("/list"))
    val names = source.getLines().filterNot(_.isEmpty).toList
    source.close()
    names
  }

  /** Scala compiler object. */
  lazy val compile = new Compiler(pluginClasses = classOf[Initialization])

  /** Source code loader. */
  lazy val source = new SourceLoader

  /** Reads title from the first (commented) line inside file.
    * @param name source file name.
    * @return test title.
    */
  def readTitle(name: String) =
    source(name)
      .content
      .mkString
      .split('\n')
      .head
      .substring("// ".length)

  /** Reads expected compilation reports from the last comment inside file.
    * @param name source file name.
    * @return expected compilation output for given file.
    */
  def readExpectedOutput(name: String) =
    source(name)
      .content
      .mkString
      .split('\n')
      .reverse
      .map(line => if (line == "//") line.concat(" ") else line)
      .takeWhile(_.startsWith("// "))
      .map(_.substring("// ".length))
      .reverse
      .mkString("\n")
}
