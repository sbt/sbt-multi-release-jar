package com.lightbend.sbt

import sbt.{ AutoPlugin, Def, PluginTrigger, Plugins, _ }
import sbt.Keys._
import sbt.plugins.JvmPlugin

object MultiReleaseJarPlugin extends AutoPlugin {

  object MultiReleaseJarKeys {
    val MultiReleaseJar = config("MultiReleaseJar") extend Compile
    val MultiReleaseJarTest = config("MultiReleaseJarTest") extend Test

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

  def jdkVersion: String = System.getProperty("java.version")
  
  override def projectSettings: Seq[Def.Setting[_]] =
    jdkVersion match {
      case "9" =>
        println(
          scala.Console.BOLD + 
            "[multi-release-jar plugin] Running using JDK9! " +
            "Will include classes and tests that require JDK9." +
            scala.Console.RESET)
        jdk9ProjectSettings
      case "1.8" | "8" => 
        println(
          scala.Console.BOLD + 
            "[multi-release-jar plugin] Running using JDK 8, " +
            "note that JDK9 classes and tests will not be included in compile/test runs." + 
            scala.Console.RESET)
        
        Seq.empty
      case _ => 
        throw new IllegalStateException("Only JDK 8 or 9 is supported by this build, because of the mult-release-jar plugin.")
    }
  
  def jdk9ProjectSettings: Seq[Def.Setting[_]] = Seq(
    compile in Compile := 
      (compile in MultiReleaseJar).dependsOn(
        compile in Compile
      ).value,
    
//    test := 
//      (test in MultiReleaseJarTest).dependsOn(
//        test in Test
//      ).value,
    
    Keys.`package` :=
      (Keys.`package` in Compile).dependsOn(
        compile in Compile, 
        compile in MultiReleaseJar
      ).value
    
  ) ++ Seq(
    // ----    testing configuration     ----
    sourceDirectories in Test ++= {
      val suffix = (jdkDirectorySuffix in MultiReleaseJar).value.replace("#", "9")
      Seq(
        (sourceDirectory in Test).value / ("java" + suffix),
        (sourceDirectory in Test).value / ("scala" + suffix)
      )
    }
    
//    , sources in MultiReleaseJarTest := {
//      val suffix = (jdkDirectorySuffix in MultiReleaseJar).value.replace("#", "9")
//      val j9TestDir = (sourceDirectory in Compile).value / ("java" + suffix)
//      val j9TestSources = (j9TestDir  ** "*").filter(_.isFile).get.toSet
//      
//      val s9TestDir = (sourceDirectory in Compile).value / ("scala" + suffix)
//      val s9TestSources = (s9TestDir  ** "*").filter(_.isFile).get.toSet
//      
//      val j9TestFiles = (j9TestSources union s9TestSources).toSeq
//      streams.value.log.warn("JDK9 Test Source files detected: " + j9TestFiles)
//      j9TestFiles
//    }
    // ---- end of testing configuration ---- 
  ) ++ inConfig(MultiReleaseJar)(Defaults.compileSettings ++ Seq(
    
    // default suffix for directories: java-jdk9, scala-jdk9
    jdkDirectorySuffix in MultiReleaseJar := "-jdk#",
    
    // here we want to generate the JDK9 files, so they target Java 9:
    javacOptions in MultiReleaseJar ++= 
      Seq("-source", "1.9", "-target", "1.9"),
    // in Compile we want to generage Java 8 compatible things though: 
    javacOptions in Compile ++= 
      Seq("-source", "1.8", "-target", "1.8"), 
      
    
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
      val j9SourcesDir = (java9Directory in MultiReleaseJar).value
      val j9Sources = (j9SourcesDir ** "*").filter(_.isFile).get.toSet
      
      val s9SourcesDir = (scala9Directory in MultiReleaseJar).value
      val s9Sources = (s9SourcesDir ** "*").filter(_.isFile).get.toSet

      val j9Files = (j9Sources union s9Sources).toSeq
      streams.value.log.debug("JDK9 Source files detected: " + j9Files)
      j9Files
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
