name := "analytics"
  
version := "1.0"
  
scalaVersion := "2.12.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

// This next line does not seem to make any difference
parallelExecution in Test := false

libraryDependencies ++= {
  val AkkaVersion = "2.5.11"
  val AkkaHttpVersion = "10.1.0"
  val slf4jVersion = "1.7.25"
  val logbackVersion = "1.2.3"
  
  Seq(
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-caching" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion,
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "ch.qos.logback" % "logback-core" % logbackVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "com.typesafe.slick" %% "slick" % "3.2.2",
    "com.github.tototoshi" %% "slick-joda-mapper" % "2.3.0",
    "joda-time" % "joda-time" % "2.7",
    "org.joda" % "joda-convert" % "1.7",
    "com.h2database" % "h2" % "1.4.196"
  )
}
