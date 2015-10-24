lazy val root = (project in file(".")).settings(
  name := "equipment",
  organization := "com.sungevity",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.11.7"
)

scalacOptions ++= Seq("-Xlint", "-deprecation", "-feature")

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

publishTo := Some("Artifactory Realm" at "https://sungevity.artifactoryonline.com/sungevity/libs-release-local")
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test",
  "com.typesafe.play" %% "play-json" % "2.3.10",
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "com.sungevity" %% "play-siren" % "0.3.0"
)

// fail if coverage is less than 100%
ScoverageSbtPlugin.ScoverageKeys.coverageMinimum := 100
ScoverageSbtPlugin.ScoverageKeys.coverageFailOnMinimum := true
