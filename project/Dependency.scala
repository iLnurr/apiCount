import sbt._

object Dependency {

  object Akka {

    val V = "2.5.6"
    val httpV = "10.0.10"

    val actor = "com.typesafe.akka" %% "akka-actor" % V
    val slf4j = "com.typesafe.akka" %% "akka-slf4j" % V
    val stream = "com.typesafe.akka" %% "akka-stream" % V
    val http = "com.typesafe.akka" %% "akka-http-core" % httpV
    val sprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % httpV
    val testKit = "com.typesafe.akka" %% "akka-testkit" % V % "test"

    def all = Seq(actor, slf4j, stream, http, sprayJson, testKit)
  }

  object Joda {
    private val time = "joda-time" % "joda-time" % "2.9.9"
    private val convert = "org.joda" % "joda-convert" % "1.9.2"

    // joda-time нормально работает со scala лишь в связке c convert
    def all = Seq(time, convert)

  }

  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val json = "org.json" % "json" % "20170516"

  object Specs2 {
    val V = "4.0.0"
    val core = "org.specs2" %% "specs2-core" % V % "test"
    val extra = "org.specs2" %% "specs2-matcher-extra" % V % "test"
    val mock = "org.specs2" %% "specs2-mock" % V % "test"

    def all = Seq(core, extra, mock)
  }

}