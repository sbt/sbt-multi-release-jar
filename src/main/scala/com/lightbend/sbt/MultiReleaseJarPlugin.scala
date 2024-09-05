package com.lightbend.sbt

import sbt.*
import sbt.Keys.*
import sbt.plugins.JvmPlugin
import sbt.{AutoPlugin, Def, PluginTrigger, Plugins}
import xsbti.VirtualFile

object MultiReleaseJarPlugin extends AutoPlugin {

  object MultiReleaseJarKeys {
    val MultiReleaseJar = config("MultiReleaseJar") extend Compile

    val keepOnlyNeeded11MultiReleaseFiles =
      taskKey[Seq[File]]("Since the JDK11 task will compile 'everything' we need to remove some classes")

    val jdkDirectorySuffix = settingKey[String](
      "The suffix added to src/main/java or src/main/java to form [scala-jdk11]. " +
        "The format should be '-jdk#' where # indicates where the version number will be injected."
    )
    val jdkJavaDirectories  = settingKey[Map[Int, File]]("Where the java multi-release sources are")
    val jdkScalaDirectories = settingKey[Map[Int, File]]("Where the java multi-release sources are")

    // These are new sbt settings that are required because original sbt settings are in-adequate, i.e.
    // we now need multiple class directories since we are dealing with multiple jdk versions
    val jdkClassDirectories = settingKey[Map[Int, File]]("Map of jdk versions to class directories ")
    val jdkJavacOptions     = settingKey[Map[Int, Seq[String]]]("Map of jdk versions to their respective javac options")
    val jdkBackendOutputs   = settingKey[Map[Int, VirtualFile]]("")

    val detectedMultiReleaseVersions =
      settingKey[Set[Int]]("Contains all of the detected multi-release jdk versions")
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

  override def projectSettings: Seq[Def.Setting[_]] = if (isJdk11) jdkProjectSettings else Seq.empty

  private def extractMultiJdkSourceDirs(mainSourceDirectory: File,
                                        sourceFolder: File,
                                        suffix: String
  ): Map[Int, File] = {
    val nestedDirs = IO.listFiles(mainSourceDirectory, (pathname: File) => pathname.isDirectory).toSet

    // Remove the currently existing sources
    val withoutScalaAndJava = nestedDirs.filterNot(nestedDir => nestedDir != sourceFolder)

    val jdkSuffixAsRegex     = suffix.replace("#", """(\d)+""")
    val sourceFolderJdkRegex = (sourceFolder.getName ++ jdkSuffixAsRegex).r

    withoutScalaAndJava
      .map { file =>
        file.getName match {
          case sourceFolderJdkRegex(jdkVersion) => Some(jdkVersion.toInt -> file)
          case _                                => None
        }
      }
      .collect { case Some(value) =>
        value
      }
      .toMap
  }

  private def extractSourcesFromMultiJdkDir(jdkVersionWithDir: Map[Int, File], tpe: String, log: Logger): Set[File] =
    jdkVersionWithDir.flatMap { case (jdkVersion, dir) =>
      val sources = (dir ** "*").filter(_.isFile).get().toSet
      log.debug(s"$tpe JDK$jdkVersion Source files detected: " + sources)
      sources
    }.toSet

  def jdkProjectSettings: Seq[Def.Setting[_]] = Seq(
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
      detectedMultiReleaseVersions.value.flatMap { jdkVersion =>
        val suffix = (MultiReleaseJar / jdkDirectorySuffix).value.replace("#", jdkVersion.toString)
        Seq(
          (Test / sourceDirectory).value / ("java" + suffix),
          (Test / sourceDirectory).value / ("scala" + suffix)
        )
      }.toList
    },

    // instead of changing unmanagedClasspath we override fullClasspath
    // since we want to make sure the "java11" classes are FIRST on the classpath
    // FIXME if possible I'd love to make this in one step, but could not figure out the right way (conversions fail)
    Test / fullClasspath ++= (MultiReleaseJar / jdkClassDirectories).value.values.toList.sorted,
    Test / fullClasspath := {
      val prev = (Test / fullClasspath).value
      // move the "jdk multi-release" classes FIRST, so they get picked up first in case of conflicts
      detectedMultiReleaseVersions.value.toList.sorted.flatMap { jdkVersion =>
        val javaClasses = prev.find(_.toString contains s"/versions/$jdkVersion").get
        Seq(javaClasses) ++ prev.filterNot(_.toString contains s"/versions/$jdkVersion")
      }
    }
  ) ++ inConfig(MultiReleaseJar)(
    (Defaults.compileSettings diff
      // Just to be safe, lets not initialize these sbt settings since multi-release-jar has its own versions
      // and we don't want to accidentally have differing behaviour
      Seq(classDirectory, backendOutput, javacOptions))
      ++ Seq(
        // default suffix for directories: e.g. java-jdk11, scala-jdk11
        MultiReleaseJar / jdkDirectorySuffix := "-jdk#",

        // here we want to generate the multi release jdk files, so they target Java JDK version:
        MultiReleaseJar / jdkJavacOptions ++= {
          detectedMultiReleaseVersions.value.map { jdkVersion =>
            val jdkArg =
              if (jdkVersion == 8)
                "1.8"
              else
                jdkVersion.toString
            jdkVersion -> Seq("-source", jdkArg, "-target", jdkArg)
          }.toMap
        },
        jdkBackendOutputs := {
          val converter = fileConverter.value
          (MultiReleaseJar / jdkClassDirectories).value.map { case (jdkVersion, dir) =>
            jdkVersion -> (converter.toVirtualFile(dir.toPath))
          }
        },
        (Compile / packageBin) / packageOptions +=
          Package.ManifestAttributes("Multi-Release" -> "true"),

        // multi release source directories
        MultiReleaseJar / jdkJavaDirectories := extractMultiJdkSourceDirs((Compile / sourceDirectory).value,
                                                                          (Compile / javaSource).value,
                                                                          (MultiReleaseJar / jdkDirectorySuffix).value
        ),
        MultiReleaseJar / jdkScalaDirectories := extractMultiJdkSourceDirs((Compile / sourceDirectory).value,
                                                                           (Compile / scalaSource).value,
                                                                           (MultiReleaseJar / jdkDirectorySuffix).value
        ),
        MultiReleaseJar / detectedMultiReleaseVersions := {
          (MultiReleaseJar / jdkJavaDirectories).value.keySet ++ (MultiReleaseJar / jdkScalaDirectories).value.keySet
        },
        // target - we kind of 'inject' our sources into the right spot:
        MultiReleaseJar / jdkClassDirectories := {
          (MultiReleaseJar / detectedMultiReleaseVersions).value.map { jdkVersion =>
            jdkVersion -> (Compile / classDirectory).value / "META-INF" / "versions" / jdkVersion.toString
          }.toMap
        },
        MultiReleaseJar / sources := {
          val log         = streams.value.log
          val javaSources = extractSourcesFromMultiJdkDir((MultiReleaseJar / jdkJavaDirectories).value, "Java", log)
          val scalaSources =
            extractSourcesFromMultiJdkDir((MultiReleaseJar / jdkScalaDirectories).value, "Scala", log)

          (javaSources union scalaSources).toSeq
        },
        MultiReleaseJar / productDirectories := {
          (MultiReleaseJar / jdkClassDirectories).value.values.toList
        },
        MultiReleaseJar / tastyFiles := Def.taskIf {
          if (ScalaArtifacts.isScala3(scalaVersion.value)) {
            val _ = (MultiReleaseJar / compile).value
            val tastyFiles = (MultiReleaseJar / jdkClassDirectories).value.values
              .flatMap(
                _.**("*.tasty").get
              )
              .toSeq
            tastyFiles.map(_.getAbsoluteFile)
          } else Nil
        }.value
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
