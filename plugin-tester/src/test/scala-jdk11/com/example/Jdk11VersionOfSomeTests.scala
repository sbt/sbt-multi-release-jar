package com.example

import org.scalatest._

class Jdk11VersionOfSomeTests extends WordSpec with Matchers {

  "src/test/scala-jdk11 tests" should {

    "only run on JDK11" in {
      System.getProperty("java.version") should ===("11")
    }

    "load the JDK11 specific class" in {
      // do nothing
    }

  }
}
