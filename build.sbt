import Dependencies._

name := """typed-chat"""
organization := "co.technius"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(inThisBuild(Seq(
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-feature",
      "-Yno-adapted-args",
      "-Xfuture"
    )
  )))
  .enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  akkaTyped.value,
  playJsonDerivedCodecs.value
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "co.technius.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "co.technius.binders._"
