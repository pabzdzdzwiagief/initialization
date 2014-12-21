// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

import sbt._
import Keys._

object Build extends sbt.Build {
  val common = Defaults.defaultSettings ++ Seq (
    version       := "0.10.3",
    scalaVersion  := "2.10.2",
    scalacOptions := Seq(
      "-deprecation",
      "-unchecked",
      "-feature"
    ),
    startYear     := Some(2013),
    licenses      := Seq(
      "The BSD 2-Clause License" → url("http://opensource.org/licenses/BSD-2-Clause")
    ),
    homepage := Some(url("https://github.com/pabzdzdzwiagief/initialization")),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (version.value.trim.endsWith("-SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <scm>
        <url>git@github.com:pabzdzdzwiagief/initialization.git</url>
        <connection>scm:git:git@github.com:pabzdzdzwiagief/initialization.git</connection>
      </scm>
      <developers>
        <developer>
          <id>pabzdzdzwiagief</id>
          <name>Aleksander Bielawski</name>
          <email>pabzdzdzwiagief@gmail.com</email>
          <url>https://github.com/pabzdzdzwiagief</url>
        </developer>
      </developers>
    )
  )

  lazy val plugin = Project(
    id = "initialization",
    base = file("."),
    settings = common ++ Seq(
      name := "initialization",
      description := "scalac plugin for initialization order checking",
      organization := "com.github.pabzdzdzwiagief",
      libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test",
      libraryDependencies += "org.springframework" % "spring-core" % "4.1.3.RELEASE" % "test"
    )
  )


  lazy val annotation = Project(
    id = "annotation",
    base = file("annotation"),
    settings = common ++ Seq(
      name := "annotation",
      description := "library dependencies for projects using the `initialization` plugin",
      organization := "com.github.pabzdzdzwiagief.initialization"
    )
  ) dependsOn(plugin) /* FIXME: Of course dependence goes the other way
                         around, but then annotations are missing on the
                         bootclasspath. */
}
