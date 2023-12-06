package com.example;

import scala.Console;

import java.io.File;
import java.io.IOException;

public class MyJavaVersionDependentImpl {
  public void print() throws IOException {
    System.out.println(
      Console.GREEN() +
        "Class from `java11`, while running Java " + System.getProperty("java.version") +
        Console.RESET());
  }
}
