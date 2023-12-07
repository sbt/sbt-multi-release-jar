package com.example

class MyJavaVersionDependentForTest {
  def loaded: String  = "jdk11"
  def running: String = System.getProperty("java.version")
}
