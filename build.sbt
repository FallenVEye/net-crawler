import sbt.Keys.resolvers

import scala.collection.immutable.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"

val akkaVersion = "2.10.2"
val akkaHttpVersion = "10.7.0"

lazy val root = (project in file("."))
    .settings(
        name := "PageCrawler",
        idePackagePrefix := Some("com.fallenveye"),
        resolvers += "Akka" at "https://repo.akka.io/maven/",
        libraryDependencies ++= Seq(
            "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
            "com.typesafe.akka" %% "akka-stream" % akkaVersion,
            "com.typesafe.akka" %% "akka-pki" % akkaVersion,
            "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
            "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
            "org.jsoup" % "jsoup" % "1.19.1"
        )
    )
