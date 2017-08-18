sbt-multi-release-jar
=====================

[![Build Status](https://travis-ci.org/sbt/sbt-multi-release-jar.svg?branch=master)](https://travis-ci.org/sbt/sbt-multi-release-jar)

Provides support for [JEP 238: Multi-Release JAR Files](https://github.com/sbt/sbt-multi-release-jar).
Note that this plugin is not building the jar itself, but just making sure to compile using the right JDK version files which live under `src/[test|main]/[java|scala]9`. The packaging into a JAR you can still us eyour favourite plugin of choice, such as sbt-assembly or others.

License
-------

This plugin is released under the **Apache 2.0 License**

Maintainer
----------

This project is maintained by Konrad [@ktoso](https://github.com/ktoso) Malawski (Akka team, Lightbend)

Contributing
------------

Yes, pull requests and opening issues is very welcome!

Please test your changes using `sbt scripted`.
