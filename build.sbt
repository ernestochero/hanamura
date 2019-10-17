name := "hanamura"

version := "0.1"

description := "GraphQL server written with sangria - Hanamura."

scalaVersion := "2.12.6"

scalacOptions ++= Seq("-deprecation", "-feature")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

val akkaVersion = "2.5.19"
val akkaHttpVersion = "10.1.8"

libraryDependencies ++= Seq(
  "io.nem" % "sdk-vertx-client" % "0.13.0-SNAPSHOT" changing(),
  "org.sangria-graphql" %% "sangria" % "1.4.2",
  "org.sangria-graphql" %% "sangria-slowlog" % "0.1.8",
  "org.sangria-graphql" %% "sangria-circe" % "1.2.1",
  "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor"  % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "de.heikoseeberger" %% "akka-http-circe" % "1.21.0",

  "io.circe" %%	"circe-core" % "0.9.3",
  "io.circe" %% "circe-parser" % "0.9.3",
  "io.circe" %% "circe-optics" % "0.9.3",

  "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0",
)

Revolver.settings
enablePlugins(JavaAppPackaging)