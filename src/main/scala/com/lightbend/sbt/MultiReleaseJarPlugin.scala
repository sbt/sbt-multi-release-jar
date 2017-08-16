package com.lightbend.sbt

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{ AutoPlugin, Def, PluginTrigger, Plugins }

object MultiReleaseJarPlugin extends AutoPlugin {

  object MultiReleaseJarKeys {
    val MultiReleaseJar = config("MultiReleaseJar") extend Compile

    val keepOnlyNeeded9MultiReleaseFiles = taskKey[Seq[File]]("Since the JDK9 task will compile 'everything' we need to remove some classes")
    
    val jdkDirectorySuffix = settingKey[String]("The suffix added to src/main/java or src/main/java to form [scala-jdk9]. " +
      "The format should be '-jdk#' where # indicates where the version number will be injected. This is for future implementations to use JDK10 etc.")
    val java9Directory = settingKey[File]("Where the java9 sources are")
    val scala9Directory = settingKey[File]("Where the scala9 sources are")
    val metaInfVersionsTargetDirectory = settingKey[File]("Where the java9 classes should be written to." +
      "Usually this is: ./target/scala-2.12/classes/META-INF/versions/9")
  }

  import MultiReleaseJarKeys._

  val autoImport = MultiReleaseJarKeys

  override def requires: Plugins = JvmPlugin

  override def trigger: PluginTrigger = allRequirements

  override def projectConfigurations = Seq(MultiReleaseJar)

  // ---------------------------------------
  // This could be taken from`java.util.jar.Attributes.MULTI_RELEASE`, but we don't want to requirerunning on JDK9! 
  val MULTI_RELEASE_KEY = "Multi-Release"
  // ---------------------------------------

  override def projectSettings = Seq(
    
    compile := 
      (compile in MultiReleaseJar)
        .dependsOn(compile in Compile)
        .value,
    
    Keys.`package` :=
      (Keys.`package` in Compile)
      .dependsOn(compile in MultiReleaseJar)
      .value
    
  ) ++ inConfig(MultiReleaseJar)(Defaults.compileSettings ++ Seq(
    jdkDirectorySuffix in MultiReleaseJar := "-jdk#",
    
    packageOptions in(Compile, packageBin) +=
      Package.ManifestAttributes("Multi-Release" -> "true"),

    // "9" source directories
    java9Directory in MultiReleaseJar := {
      val suffix = (jdkDirectorySuffix in MultiReleaseJar).value.replace("#", "9")
      (sourceDirectory in Compile).value / ("java" + suffix)
    },
    scala9Directory in MultiReleaseJar := {
      val suffix = (jdkDirectorySuffix in MultiReleaseJar).value.replace("#", "9")
      (sourceDirectory in Compile).value / ("scala" + suffix)
    },

    // target - we kind of 'inject' our sources into the right spot:
    metaInfVersionsTargetDirectory := {
      (crossTarget in Compile).value / "classes" / "META-INF" / "versions" / "9"
    },
    
    classDirectory in MultiReleaseJar := 
      (crossTarget in Compile).value / "classes" / "META-INF" / "versions" / "9",
    
    sources in MultiReleaseJar := {
      val baseDir = (sourceDirectory in Compile).value

      val j9SourcesDir = (java9Directory in MultiReleaseJar).value
      val j9Sources = (j9SourcesDir ** "*").filter(_.isFile).get.map(compareByInternalFilePath(baseDir)).toSet

      val s9SourcesDir = (scala9Directory in MultiReleaseJar).value
      val s9Sources = (s9SourcesDir ** "*").filter(_.isFile).get.map(compareByInternalFilePath(baseDir)).toSet

      val all9Sources: Set[FileComparableByName] = 
        j9Sources union s9Sources
      
      // the outside world wants a plain File of course though:
      all9Sources.map(_.file).toSeq
    },
    
    crossTarget in MultiReleaseJar := metaInfVersionsTargetDirectory.value,
    target in MultiReleaseJar := metaInfVersionsTargetDirectory.value
  ))

  def compareByInternalFilePath(baseDir: File)(f: File): FileComparableByName = 
    new FileComparableByName(baseDir, f)
  
  final class FileComparableByName(baseDir: File, val file: File) {
    val internalPath = {
      val rel = file.relativeTo(baseDir).get.toString // `java-jdk9/com/example/MyJavaVersionDependentImpl.java`
      rel.substring(rel.indexOf('/') + 1) // just `com/example/MyJavaVersionDependentImpl.java`
    }
    
    override def toString: String = {
      val dir = file.relativeTo(baseDir).get.toString.takeWhile(_ != '/') 
      s"[$dir] $internalPath"
    }
    
    override def equals(other: Any): Boolean = other match {
      case that: FileComparableByName => internalPath == that.internalPath
      case _ => false
    }
    
    override def hashCode(): Int = {
      val state = Seq(internalPath)
      state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
    }
  } 
}
