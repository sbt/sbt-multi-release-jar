sbt-multi-release-jar
=====================

[![Build Status](https://travis-ci.org/sbt/sbt-multi-release-jar.svg?branch=master)](https://travis-ci.org/sbt/sbt-multi-release-jar)

Provides support for [JEP 238: Multi-Release JAR Files](http://openjdk.java.net/jeps/238).
Note that this plugin is not building the jar itself, but just making sure to compile using the right JDK version files which live under `src/[test|main]/[java|scala]9`. The packaging into a JAR you can still us eyour favourite plugin of choice, such as sbt-assembly or others.

License
-------

This plugin is released under the **Apache 2.0 License**

Contributing
------------

Yes, pull requests and opening issues is very welcome!

Please test your changes using `sbt scripted`.
