// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

import sbt._
import Keys._

object Build extends sbt.Build {
  val common = Defaults.defaultSettings ++ Seq (
    version       := "0.1.0",
    scalaVersion  := "2.10.1",
    scalacOptions := Seq(
      "-deprecation",
      "–unchecked",
      "-explaintypes",
      "-feature"
    ),
    startYear     := Some(2013),
    licenses      := Seq(
      "The BSD 2-Clause License" → url("http://opensource.org/licenses/BSD-2-Clause")
    ),
    pomExtra := (
      <developers>
        <developer>
          <id>pabzdzdzwiagief</id>
          <name>Aleksander Bielawski</name>
          <email>pabzdzdzwiagief@gmail.com</email>
        </developer>
      </developers>
    )
  )

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = common ++ Seq(
      name := "initialization",
      description := "scalac plugin for initialization order checking",
      organization := "com.github.pabzdzdzwiagief.initialization"
    )
  ) aggregate (plugin, annotations)

  lazy val plugin = Project(
    id = "plugin",
    base = file("plugin"),
    settings = common ++ Seq(
      name := "plugin",
      description := "the `initialization` plugin itself",
      organization := "com.github.pabzdzdzwiagief.initialization.plugin",
      libraryDependencies +=  "org.scala-lang" % "scala-compiler" % "2.10.1",
      libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test",
      libraryDependencies += "com.novocode" % "junit-interface" % "0.9" % "test->default"
    )
  ) dependsOn(annotations)

  lazy val annotations = Project(
    id = "annotations",
    base = file("annotations"),
    settings = common ++ Seq(
      name := "annotations",
      description := "library dependencies for projects using the `initialization` plugin",
      organization := "com.github.pabzdzdzwiagief.initialization.annotations"
    )
  )
}
