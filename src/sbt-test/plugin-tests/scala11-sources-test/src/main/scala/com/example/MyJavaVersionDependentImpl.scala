package com.example

class MyJavaVersionDependentImpl {
  def print(): Unit =
    println(
      "Scala Class from `java{6,7,8}`, " +
        "while running [Java " + System.getProperty("java.version") + "]"
    )
}
