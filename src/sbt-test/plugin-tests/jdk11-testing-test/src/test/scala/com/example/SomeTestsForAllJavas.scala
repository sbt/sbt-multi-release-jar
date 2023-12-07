package com.example

import org.scalatest._

class SomeTestsForAllJavas extends WordSpec with Matchers {

  "src/test/scala tests" should {

    "work under all JDKs" in {
      val impl = new MyJavaVersionDependentForTest
      info(s"Running on [Java ${impl.running}]")
    }
  }

}
