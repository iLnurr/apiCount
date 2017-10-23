name := "apiCount"

version := "0.1"

import Dependency._

libraryDependencies ++= Akka.all ++ Joda.all ++ Specs2.all ++ Seq(logback, json)