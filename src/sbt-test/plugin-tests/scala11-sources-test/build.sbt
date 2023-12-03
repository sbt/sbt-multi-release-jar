//enablePlugins(MultiReleaseJarPlugin)

TaskKey[Unit]("check") := {
  validateClassMajorVersion("", "major version: 52") // java 8
  validateClassMajorVersion("META-INF/versions/11/", "major version: 52") // Scala 2.12 still generates java 8 class format
}

def validateClassMajorVersion(subdir: String, expectedVersion: String) = {
  val file = "target/scala-2.12/classes/" + subdir + "com/example/MyJavaVersionDependentImpl.class"
  
  import scala.sys.process._
  val out = Process("javap", Seq("-v", file)).!!
  val classVersion = out.split("\n").find(_.contains("major version")).map(_.trim).getOrElse("unknown")
  
  println(s"[check] File ${file} is in version: ${classVersion}")
  
  if (classVersion == expectedVersion) ()
  else sys.error(
    s"Found class version [$classVersion], " +
    s"expected version: [$expectedVersion], " +
      s"for file: $file")
}
