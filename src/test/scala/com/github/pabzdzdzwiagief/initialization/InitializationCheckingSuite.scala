// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import tools.nsc.io.File
import java.io.{File => JFile}

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter

/** Tests if compilation outputs are matching those expected. */
class InitializationCheckingSuite extends FunSuite with BeforeAndAfter {
  for (url <- testFiles) {
    val path = url.toString
    val shortPath = url.getPath.split("/").takeRight(2).mkString("/")
    val title = s"${readTitle(path)} in $shortPath"
    val ignorePrefix = "*not implemented* "
    val testOrIgnore = if (title.startsWith(ignorePrefix)) {
      ignore(title.substring(ignorePrefix.length)) _
    } else {
      test(title) _
    }
    testOrIgnore {
      assertResult(readExpectedOutput(path)) {
        compile(source(path))
      }
    }
  }

  test("Initialization order information is persisted between compiler runs") {
    def url(file: String) =
      getClass.getResource(s"/separate-runs/$file.scala").toString
    assert(compile(source(url("base"))).isEmpty)
    assertResult(readExpectedOutput(url("inheritance"))) {
      compile(source(url("inheritance")))
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
  lazy val testFiles =
    new PathMatchingResourcePatternResolver()
      .getResources("classpath*:examples/*/*.scala")
      .map(_.getURL)

  /** Scala compiler object. */
  def compile = new Compiler(pluginClasses = classOf[Initialization])

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
