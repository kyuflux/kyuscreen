name := """kyuscreen"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  anorm,
  cache,
  ws
)

libraryDependencies += "commons-codec" % "commons-codec" % "1.10"
