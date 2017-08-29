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

// ---------------------------------------------------------------------------------------------------------------------
// sbt-scripted settings
val scriptedSettings = Seq(
  scriptedLaunchOpts += s"-Dproject.version=${version.value}",
  scriptedBufferLog := false
) 

// ---------------------------------------------------------------------------------------------------------------------
// main settings 
commonSettings

scriptedSettings

name := "sbt-multi-release-jar"

crossSbtVersions := Vector("0.13.16", "1.0.0")
// ---------------------------------------------------------------------------------------------------------------------
// publishing settings

sbtPlugin := true
publishTo := Some(Classpaths.sbtPluginReleases)
publishMavenStyle := false

// bintray config
bintrayOrganization := Some("sbt")
bintrayRepository := "sbt-plugin-releases"
bintrayPackage := "sbt-plugin-releases"
