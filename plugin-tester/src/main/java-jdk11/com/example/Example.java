package com.example;

import scala.Console;

public class Example {
  public static void main(String[] args) {
    System.out.println(
      Console.GREEN() +
        "Class from `java11`, while running [Java " + System.getProperty("java.version") + "]" +
        Console.RESET()
    );
  }
}
