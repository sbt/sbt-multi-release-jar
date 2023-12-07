package com.lightbend.sbt

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{AutoPlugin, Def, PluginTrigger, Plugins}

object MultiReleaseJarPlugin extends AutoPlugin {

  object MultiReleaseJarKeys {
    val MultiReleaseJar = config("MultiReleaseJar") extend Compile

    val keepOnlyNeeded11MultiReleaseFiles =
      taskKey[Seq[File]]("Since the JDK11 task will compile 'everything' we need to remove some classes")

    val jdkDirectorySuffix = settingKey[String](
      "The suffix added to src/main/java or src/main/java to form [scala-jdk11]. " +
        "The format should be '-jdk#' where # indicates where the version number will be injected."
    )
    val java11Directory  = settingKey[File]("Where the java11 sources are")
    val scala11Directory = settingKey[File]("Where the scala11 sources are")
    val metaInfVersionsTargetDirectory = settingKey[File](
      "Where the java11 classes should be written to." +
        "Usually this is: ./target/scala-2.12/classes/META-INF/versions/11"
    )
  }

  import MultiReleaseJarKeys._

  val autoImport = MultiReleaseJarKeys

  override def requires: Plugins = JvmPlugin

  override def trigger: PluginTrigger = allRequirements

  override def projectConfigurations = Seq(MultiReleaseJar)

  // ---------------------------------------
  // This could be taken from`java.util.jar.Attributes.MULTI_RELEASE`, but we don't want to require running on JDK11!
  val MULTI_RELEASE_KEY = "Multi-Release"
  // ---------------------------------------

  val jdkVersion: String = System.getProperty("java.version")

  private[this] val isJdk11 =
    if (jdkVersion startsWith "11") true
    else if (jdkVersion startsWith "1.8") false
    else
      throw new IllegalStateException(
        s"Only JDK 8 or 11 is supported by this build, because of the multi-release-jar plugin. Detected version: $jdkVersion"
      )

  override def globalSettings = {
    def log(s: String) =
      println(scala.Console.BOLD + "[multi-release-jar plugin] " + s + scala.Console.RESET)
    if (isJdk11)
      log("Running using JDK11! Will include classes and tests that require JDK11.")
    else
      log("Running using JDK 8, note that JDK11 classes and tests will not be included in compile/test runs.")
    super.globalSettings
  }

  override def projectSettings: Seq[Def.Setting[_]] = if (isJdk11) jdk11ProjectSettings else Seq.empty

  def jdk11ProjectSettings: Seq[Def.Setting[_]] = Seq(
    compile :=
      (MultiReleaseJar / compile)
        .dependsOn(
          Compile / compile
        )
        .value,
    Keys.`package` :=
      (Compile / Keys.`package`)
        .dependsOn(
          Compile / compile,
          MultiReleaseJar / compile
        )
        .value,
    Keys.publish :=
      Keys.publish
        .dependsOn(
          MultiReleaseJar / compile
        )
        .value,
    Keys.publishLocal :=
      Keys.publishLocal
        .dependsOn(
          MultiReleaseJar / compile
        )
        .value
  ) ++ Seq(
    Test / unmanagedSourceDirectories ++= {
      val suffix = (MultiReleaseJar / jdkDirectorySuffix).value.replace("#", "11")
      Seq(
        (Test / sourceDirectory).value / ("java" + suffix),
        (Test / sourceDirectory).value / ("scala" + suffix)
      )
    },

    // instead of changing unmanagedClasspath we override fullClasspath
    // since we want to make sure the "java11" classes are FIRST on the classpath
    // FIXME if possible I'd love to make this in one step, but could not figure out the right way (conversions fail)
    Test / fullClasspath += (MultiReleaseJar / classDirectory).value,
    Test / fullClasspath := {
      val prev = (Test / fullClasspath).value
      // move the "java11" classes FIRST, so they get picked up first in case of conflicts
      val j11Classes = prev.find(_.toString contains "/versions/11").get
      Seq(j11Classes) ++ prev.filterNot(_.toString contains "/versions/11")
    }

//    Test / fullClasspath := {
//      val prev = (Test / fullClasspath).value
//    }

  ) ++ inConfig(MultiReleaseJar)(
    Defaults.compileSettings ++ Seq(
      // default suffix for directories: java-jdk11, scala-jdk11
      MultiReleaseJar / jdkDirectorySuffix := "-jdk#",

      // here we want to generate the JDK11 files, so they target Java 11:
      MultiReleaseJar / javacOptions ++=
        Seq("-source", "11", "-target", "11"),
      // in Compile we want to generate Java 8 compatible things though:
      Compile / javacOptions ++=
        Seq("-source", "1.8", "-target", "1.8"),
      (Compile / packageBin) / packageOptions +=
        Package.ManifestAttributes("Multi-Release" -> "true"),

      // "11" source directories
      MultiReleaseJar / java11Directory := {
        val suffix = (MultiReleaseJar / jdkDirectorySuffix).value.replace("#", "11")
        (Compile / sourceDirectory).value / ("java" + suffix)
      },
      MultiReleaseJar / scala11Directory := {
        val suffix = (MultiReleaseJar / jdkDirectorySuffix).value.replace("#", "11")
        (Compile / sourceDirectory).value / ("scala" + suffix)
      },

      // target - we kind of 'inject' our sources into the right spot:
      metaInfVersionsTargetDirectory := {
        (Compile / classDirectory).value / "META-INF" / "versions" / "11"
      },
      MultiReleaseJar / classDirectory := metaInfVersionsTargetDirectory.value,
      MultiReleaseJar / sources := {
        val j11SourcesDir = (MultiReleaseJar / java11Directory).value
        val j11Sources    = (j11SourcesDir ** "*").filter(_.isFile).get.toSet

        val s11SourcesDir = (MultiReleaseJar / scala11Directory).value
        val s11Sources    = (s11SourcesDir ** "*").filter(_.isFile).get.toSet

        val j11Files = (j11Sources union s11Sources).toSeq
        streams.value.log.debug("JDK11 Source files detected: " + j11Files)
        j11Files
      }
    )
  )

  def compareByInternalFilePath(baseDir: File)(f: File): FileComparableByName =
    new FileComparableByName(baseDir, f)

  final class FileComparableByName(baseDir: File, val file: File) {
    val internalPath = {
      val rel = file.relativeTo(baseDir).get.toString // `java-jdk11/com/example/MyJavaVersionDependentImpl.java`
      rel.substring(rel.indexOf('/') + 1) // just `com/example/MyJavaVersionDependentImpl.java`
    }

    override def toString: String = {
      val dir = file.relativeTo(baseDir).get.toString.takeWhile(_ != '/')
      s"[$dir] $internalPath"
    }

    override def equals(other: Any): Boolean = other match {
      case that: FileComparableByName => internalPath == that.internalPath
      case _                          => false
    }

    override def hashCode(): Int = {
      val state = Seq(internalPath)
      state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
    }
  }
}
