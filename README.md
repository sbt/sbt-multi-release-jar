sbt-multi-release-jar
=====================

 [![Continuous Integration](https://github.com/sbt/sbt-multi-release-jar/actions/workflows/ci.yml/badge.svg)](https://github.com/sbt/sbt-multi-release-jar/actions/workflows/ci.yml)

Provides support for [JEP 238: Multi-Release JAR Files](http://openjdk.java.net/jeps/238).
Note that this plugin is not building the jar itself, but just making sure to compile using the right JDK version files which live under `src/[test|main]/[java|scala]-jdk11`. The packaging into a JAR you can still use your favourite plugin of choice, such as sbt-assembly or others.

Usage
-----

```
addSbtPlugin("com.lightbend.sbt" % "sbt-multi-release-jar" % "0.1.2")
```

This plugin allows you to keep "Java 11 only" sources in:

- `src/main/scala-jdk11` 
- `src/main/java-jdk11` 
- `src/test/scala-jdk11` 
- `src/test/java-jdk11` 

which will only be compiled (and tests would only run) if running on JDK11.

**The purpose of this plugin** though is not only that, it is to be able to `package`
such classes into the special "multi-release jar format" defined by JEP-238 (see link above).

For example this allows you to implement a specific class once using pre-JDK11 features,
and also separately using the JDK11+ features (like varhandles or other library additions).
Assuming the such implemented class is `akka.stream.impl.MagicEngine` for example, you'd 
implement it in `src/main/scala/akka/stream/impl/MagicEngine.scala` 
and `src/main/scala-jdk11/akka/stream/impl/MagicEngine.scala`, and the resulting JAR will end up containing:

```
akka/stream/impl/MagicEngine.class
...
META-INF/versions/11/akka/stream/impl/MagicEngine.class
```

In runtime, when Java 11 is used, the `META-INF/versions/11/...` class is automatically loaded instead of the 
"normal class". This is a feature of the Java 9 Runtime and is transparent to the application itself. 
Java runtimes prior to version 9 do not know about the special meaning of these directories, and thus will 
simply load the "normal class" from the root of the JAR.

This summarises the main use-case of Multi-Release JARs, however feel free to read more in the 
[JEP 238: Multi-Release JAR Files](http://openjdk.java.net/jeps/238).

Maintainer
----------

This project is maintained by Konrad [@ktoso](https://github.com/ktoso) Malawski (Akka team, Lightbend)

Contributing
------------

Yes, pull requests and opening issues are very welcome!

Please test your changes using `sbt scripted`.

License
-------

This plugin is released under the **Apache 2.0 License**
