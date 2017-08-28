package com.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ListAllClasses {
  public static void main(String[] args) throws IOException {

    System.out.println("CLASSES: ");
    final File testBase = new File(new File(".", "target"), "scala-2.12/classes");
    System.out.println("Class directory = " + testBase.getAbsolutePath());
    Files.walk(testBase.toPath())
      .filter(Files::isRegularFile)
      .forEach(System.out::println);
        
    System.out.println("TEST CLASSES: ");
    final File base = new File(new File(".", "target"), "scala-2.12/test-classes");
    System.out.println("Test class directory = " + base.getAbsolutePath());
    Files.walk(base.toPath())
      .filter(Files::isRegularFile)
      .forEach(System.out::println);
  }
}
