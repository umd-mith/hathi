import sbt._, Keys._
import sbtassembly.Plugin._, AssemblyKeys._

object HathiBuild extends Build {
  lazy val core: Project = Project(
    id = "hathi-core",
    base = file("core"),
    dependencies = Seq(util),
    settings = commonSettings ++ assemblySettings
  )

  lazy val util: Project = Project(
    id = "hathi-util",
    base = file("util"),
    settings = commonSettings
  )

  lazy val root: Project = Project(
    id = "hathi",
    base = file("."),
    settings = commonSettings
  ).aggregate(core)

  def commonSettings = Defaults.defaultSettings ++ Seq(
    organization := "edu.umd.mith",
    version := "0.0.0-SNAPSHOT",
    scalaVersion := "2.10.4",
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      "Index Data" at "http://maven.indexdata.com/",
      "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
    ),
    javaOptions += "-Xmx4G",
    scalacOptions := Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-Xlint"
    ),
    scalacOptions in Test ++= Seq("-Yrangepos"),
    libraryDependencies ++= testDependencies ++ Seq(
      "commons-lang" % "commons-lang" % "2.6",
      "io.argonaut" %% "argonaut" % "6.0.4",
      "joda-time" % "joda-time" % "2.3",
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
      "org.joda" % "joda-convert" % "1.6",
      "org.marc4j" % "marc4j" % "2.6-SNAPSHOT",
      "org.slf4j" % "slf4j-simple" % "1.7.6",
      "org.scalesxml" %% "scales-xml" % "0.6.0-M1",
      "org.scalaz" %% "scalaz-core" % "7.0.6",
      "org.scalaz" %% "scalaz-concurrent" % "7.0.6",
      "org.scalaz.stream" %% "scalaz-stream" % "0.4.1"
    )
  )

  val testDependencies = Seq(
    "org.scalacheck" %% "scalacheck" % "1.11.3" % "test",
    "org.specs2" %% "specs2" % "2.3.10" % "test",
    "org.typelevel" %% "scalaz-specs2" % "0.1.5" % "test"
  )
}
