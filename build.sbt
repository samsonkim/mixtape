name := "mixtape"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
  "net.liftweb" %% "lift-json" % "3.4.3",
  "org.wvlet.airframe"  %% "airframe-launcher" % "19.11.1",
  "org.scalactic" %% "scalactic" % "3.2.2",
  "org.scalatest" %% "scalatest" % "3.2.2" % "test",
  "org.scalatest" %% "scalatest-flatspec" % "3.2.2" % "test"
)

enablePlugins(PackPlugin)
packMain := Map("mixtape" -> "com.mixtape.MixtapeApp")