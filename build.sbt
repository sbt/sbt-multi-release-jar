//import bintray.Keys._
import java.io.FileInputStream
import java.util.Properties

val commonSettings = Seq(
  organization := "com.lightbend.sbt",

  crossSbtVersions := Vector("0.13.16", "1.0.0"),

  scalacOptions ++= List(
    "-unchecked",
    "-deprecation",
    "-language:_",
    "-encoding", "UTF-8"
  )
)

// sbt-scripted settings
val myScriptedSettings = Seq(
  scriptedLaunchOpts += s"-Dproject.version=${version.value}",
  scriptedBufferLog := false
) 

// ---------------------------------------------------------------------------------------------------------------------

commonSettings

myScriptedSettings

name := "sbt-multi-release-jar"
    
sbtPlugin := true
publishTo := Some(Classpaths.sbtPluginReleases)
publishMavenStyle := false

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
//bintrayPublishSettings
//repository in bintray := "sbt-plugins"
//bintrayOrganization in bintray := None

