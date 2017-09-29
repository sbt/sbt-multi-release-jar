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

  val jdkVersion: String = System.getProperty("java.version")

  private[this] val isJdk9 =
    if (jdkVersion startsWith "9") true
    else if (jdkVersion startsWith "1.8") false
    else throw new IllegalStateException(s"Only JDK 8 or 9 is supported by this build, because of the mult-release-jar plugin. Detected version: $jdkVersion")

  override def globalSettings = {
    def log(s: String) =
      println(scala.Console.BOLD + "[multi-release-jar plugin] " + s + scala.Console.RESET)
    if (isJdk9)
      log("Running using JDK9! Will include classes and tests that require JDK9.")
    else
      log("Running using JDK 8, note that JDK9 classes and tests will not be included in compile/test runs.")
    super.globalSettings
  }

  override def projectSettings: Seq[Def.Setting[_]] = if (isJdk9) jdk9ProjectSettings else Seq.empty

  def jdk9ProjectSettings: Seq[Def.Setting[_]] = Seq(
    compile :=
      (compile in MultiReleaseJar).dependsOn(
        compile in Compile
      ).value,

    Keys.`package` :=
      (Keys.`package` in Compile).dependsOn(
        compile in Compile,
        compile in MultiReleaseJar
      ).value,
    
    Keys.publish :=
      (Keys.publish).dependsOn(
        compile in MultiReleaseJar
      ).value,
    
    Keys.publishLocal :=
      (Keys.publishLocal).dependsOn(
        compile in MultiReleaseJar
      ).value

  ) ++ Seq(
    unmanagedSourceDirectories in Test ++= {
      val suffix = (jdkDirectorySuffix in MultiReleaseJar).value.replace("#", "9")
      Seq(
        (sourceDirectory in Test).value / ("java" + suffix),
        (sourceDirectory in Test).value / ("scala" + suffix)
      )
    },

    // instead of changing unmanagedClasspath we override fullClasspath
    // since we want to make sure the "java9" classes are FIRST on the classpath
    // FIXME if possible I'd love to make this in one step, but could not figure out the right way (conversions fail)
    fullClasspath in Test += (classDirectory in MultiReleaseJar).value,
    fullClasspath in Test := {
      val prev = (fullClasspath in Test).value
      // move the "java9" classes FIRST, so they get picked up first in case of conflicts
      val j9Classes = prev.find(_.toString contains "/versions/9").get
      Seq(j9Classes) ++ prev.filterNot(_.toString contains "/versions/9")
    }

//    fullClasspath in Test := {
//      val prev = (fullClasspath in Test).value
//    }

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
      (classDirectory in Compile).value / "META-INF" / "versions" / "9"
    },

    classDirectory in MultiReleaseJar := metaInfVersionsTargetDirectory.value,

    sources in MultiReleaseJar := {
      val j9SourcesDir = (java9Directory in MultiReleaseJar).value
      val j9Sources = (j9SourcesDir ** "*").filter(_.isFile).get.toSet

      val s9SourcesDir = (scala9Directory in MultiReleaseJar).value
      val s9Sources = (s9SourcesDir ** "*").filter(_.isFile).get.toSet

      val j9Files = (j9Sources union s9Sources).toSeq
      streams.value.log.debug("JDK9 Source files detected: " + j9Files)
      j9Files
    }
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
