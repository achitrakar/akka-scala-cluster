name := "akka-scala-cluster"

version := "0.1"

scalaVersion := "2.12.10"

val akkaVersion = "2.5.32"
val akkaManagementVersion = "1.0.5"


addSbtPlugin("cluster" % "cluster-demo" % "0.15.0")

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic"  % "1.1.11",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.lightbend.akka.management" %% "akka-management" % akkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
  "org.scalatest" %% "scalatest" % "3.2.5",
  "org.scalatest" %% "scalatest" % "3.2.5" % "test",
)