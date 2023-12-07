package com.example

import java.io.File
import java.nio.file.Files

object MainScalaRunner extends App {

  new MyJavaVersionDependentImpl().print()

  val base = new File(new File(".", "target"), "scala-2.12/classes")
  println("Working directory = " + base.getAbsolutePath)

  Files
    .walk(base.toPath)
    .forEach(p => println(p))
}
