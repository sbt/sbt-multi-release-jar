package com.example

import org.scalatest._

class Jdk9VersionOfSomeTests extends WordSpec with Matchers {

  "MyJavaVersionDependentImpl" should {

    "work under JDK9" in {
      val impl = new MyJavaVersionDependentImpl
      impl.loaded should ===("jdk9")
      // impl.running should ===("1.9")
    }

  }
}
