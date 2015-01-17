lazy val initialization = (project in file(".")).
  settings(common: _*).
  settings(
    name := "initialization",
    description := "scalac plugin for initialization order checking",
    organization := "com.github.pabzdzdzwiagief",
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.3" % "test",
    libraryDependencies += "org.springframework" % "spring-core" % "4.1.3.RELEASE" % "test",
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.copy(`classifier` = Some("assembly"))
    }
  ).
  settings(addArtifact(artifact in (Compile, assembly), assembly): _*).
  dependsOn(annotation)

lazy val annotation = (project in file("annotation")).
  settings(common: _*).
  settings(
    name := "annotation",
    description := "library dependencies for projects using the `initialization` plugin",
    organization := "com.github.pabzdzdzwiagief.initialization",
    javacOptions in (Compile, doc) ++= Seq("-private")
  )

lazy val common = Seq (
  version       := "0.11.0-rc.1",
  scalaVersion  := "2.10.4",
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
