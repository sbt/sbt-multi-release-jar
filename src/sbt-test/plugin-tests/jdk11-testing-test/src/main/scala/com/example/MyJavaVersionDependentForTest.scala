package com.example

class MyJavaVersionDependentForTest {
  def loaded: String = "jdk8"
  def running: String = System.getProperty("java.version")
}
