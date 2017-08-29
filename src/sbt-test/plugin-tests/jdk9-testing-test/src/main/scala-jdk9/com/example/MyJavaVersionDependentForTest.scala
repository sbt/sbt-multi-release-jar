package com.example

class MyJavaVersionDependentForTest {
  def loaded: String = "jdk9"
  def running: String = System.getProperty("java.version")
}
