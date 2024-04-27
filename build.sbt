import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "realworld-backend-scala-akka"
  )

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

val AkkaVersion = "2.9.2"
val AkkaHttpVersion = "10.6.2"
val LogbackVersion = "1.5.6"
val ScalikeJdbcVersion = "4.2.1"
val SlickVersion = "3.5.1"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % LogbackVersion,
  "com.github.jwt-scala" %% "jwt-core" % "10.0.0",
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-jwt" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.4.0",
  "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.slick" %% "slick" % SlickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
  "io.github.nremond" %% "pbkdf2-scala" % "0.7.2",
  "org.postgresql" % "postgresql" % "42.7.3",
  "org.scalikejdbc" %% "scalikejdbc" % ScalikeJdbcVersion,
  "org.scalikejdbc" %% "scalikejdbc-config" % ScalikeJdbcVersion
)
