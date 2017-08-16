package com.example;

import scala.Console;

public class Example {
  public static void main(String[] args) {
    System.out.println(
      Console.RED() +
        "Class from `java{6,7,8}`, while running [Java " + System.getProperty("java.version") + "]" +
        Console.RESET());
  }
}
