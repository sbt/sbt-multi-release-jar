val commonSettings = Seq(
  organization := "com.lightbend.sbt",
  scalacOptions ++= List(
    "-unchecked",
    "-deprecation",
    "-language:_",
    "-encoding",
    "UTF-8"
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

// ---------------------------------------------------------------------------------------------------------------------
// publishing settings

sbtPlugin := true
enablePlugins(SbtPlugin)
//publishTo := Some(Classpaths.sbtPluginReleases) // THIS IS BAD IN THE CURRENT PLUGIN VERSION
publishMavenStyle := false

ThisBuild / githubWorkflowJavaVersions := List(JavaSpec.temurin("11"))

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(name = Some("Build project"), commands = List("test", "scripted"))
)

ThisBuild / githubWorkflowPublishTargetBranches := Seq() // Disable publishing for now
