import com.lightbend.sbt.MultiReleaseJarPlugin

name := "plugin-tester"

enablePlugins(MultiReleaseJarPlugin)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
