package com.example

import scala.Console

import java.io.File
import java.io.IOException

class MyJavaVersionDependentImpl {
  def print(): Unit = {
    println(Console.GREEN + "Class from `java11`, while running Java " + System.getProperty("java.version") + Console.RESET)
  }
}
