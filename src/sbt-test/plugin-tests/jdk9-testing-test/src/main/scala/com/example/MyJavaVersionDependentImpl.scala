package com.example

class MyJavaVersionDependentImpl {
  def loaded: String = "jdk8"
  def running: String = System.getProperty("java.version")
}
