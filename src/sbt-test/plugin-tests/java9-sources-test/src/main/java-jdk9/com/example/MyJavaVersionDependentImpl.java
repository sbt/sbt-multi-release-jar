package com.example;

import java.io.File;
import java.io.IOException;

public class MyJavaVersionDependentImpl {
  public void print() throws IOException {
    System.out.println("Class from `java9`, while running Java " + System.getProperty("java.version"));
  }
}
