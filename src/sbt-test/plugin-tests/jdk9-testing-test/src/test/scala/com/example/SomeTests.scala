package com.example

import org.scalatest._

class SomeTests extends WordSpec with Matchers {

  "MyJavaVersionDependentImpl" should {

    "work under JDK8" in {
      val impl = new MyJavaVersionDependentImpl
      impl.loaded should ===("jdk8")
      // impl.running should ===("1.8")
    }

  }
}
