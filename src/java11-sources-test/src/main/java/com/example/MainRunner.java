package com.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class MainRunner {
  public static void main(String[] args) throws IOException {
    new MyJavaVersionDependentImpl().print();
    
    final File base = new File(new File(".", "target"), "scala-2.12/classes");
    System.out.println("Working directory = " + base.getAbsolutePath());
    Files.walk(base.toPath())
      .filter(Files::isRegularFile)
      .forEach(System.out::println);
  }
}
