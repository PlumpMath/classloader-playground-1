import sbt._
import Keys._
import Process._
import sbtassembly.Plugin._
import AssemblyKeys._

object MiniboxingBuild extends Build {

  val scalaVer = "2.10.1-SNAPSHOT"

  val defaults = Defaults.defaultSettings ++ assemblySettings ++ Seq(
    scalaVersion := scalaVer,
    scalaBinaryVersion := "2.10",
    scalaSource in Compile <<= baseDirectory(_ / "src"),
    scalaSource in Test <<= baseDirectory(_ / "test"),
    resourceDirectory in Compile <<= baseDirectory(_ / "resources"),
    scalacOptions ++= Seq(),

    unmanagedSourceDirectories in Compile <<= (scalaSource in Compile)(Seq(_)),
    unmanagedSourceDirectories in Test <<= (scalaSource in Test)(Seq(_)),
    //http://stackoverflow.com/questions/10472840/how-to-attach-sources-to-sbt-managed-dependencies-in-scala-ide#answer-11683728
    com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseKeys.withSource := true,

    // this should work but it doesn't:
    resolvers in ThisBuild ++= Seq(
      ScalaToolsSnapshots,
      "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases"
    ),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v"),
    parallelExecution in Test := false,

    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.10.0" % "test",
      "org.ow2.asm" % "asm" % "4.0",
      "org.ow2.asm" % "asm-tree" % "4.0",
      "com.novocode" % "junit-interface" % "0.10-M2" % "test"
    ),
    parallelExecution in Test := false
  )

  lazy val _classloader = Project(id = "classloader",        base = file(".")) aggregate (loader, tests)
  lazy val loader       = Project(id = "classloader-loader", base = file("components/loader"), settings = defaults)
  lazy val tests        = Project(id = "classloader-test",   base = file("components/test"),   settings = defaults)
}
