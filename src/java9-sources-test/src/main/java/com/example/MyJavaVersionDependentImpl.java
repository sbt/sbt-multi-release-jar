package com.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MyJavaVersionDependentImpl {
  public void print() throws IOException {
    System.out.println("Class from `java{6,7,8}`, while running [Java " + System.getProperty("java.version") + "]");
  }
}
