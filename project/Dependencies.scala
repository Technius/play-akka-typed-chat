import sbt._

object Dependencies {
  lazy val akkaTyped = Def.setting("com.typesafe.akka" %% "akka-typed" % "2.5.6")

  lazy val playJsonDerivedCodecs = Def.setting("org.julienrf" %% "play-json-derived-codecs" % "4.0.0")

  lazy val webjarPurecss = Def.setting("org.webjars.npm" % "purecss" % "1.0.0")
}
