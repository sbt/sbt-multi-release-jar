package com.example

class MyJavaVersionDependentImpl {
  def loaded: String = "jdk9"
  def running: String = System.getProperty("java.version")
}
