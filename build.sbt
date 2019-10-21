name := "hanamura"

version := "0.1"

description := "GraphQL server written with sangria - Hanamura."

scalaVersion := "2.12.10"

scalacOptions ++= Seq("-deprecation", "-feature")
scalacOptions ++= Seq("-Ypartial-unification")
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
  //"io.circe" %% "circe-parser" % "0.9.3",
  "io.circe" %% "circe-optics" % "0.9.3",

  "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0",
  "com.github.ghostdogpr" %% "caliban" % "0.1.0",
  "dev.zio"       %% "zio-interop-cats"    % "2.0.0.0-RC6",
  "org.typelevel" %% "cats-effect"         % "2.0.0",
  "org.http4s"    %% "http4s-dsl"          % "0.20.6",
  "org.http4s"    %% "http4s-circe"        % "0.20.6",
  "org.http4s"    %% "http4s-blaze-server" % "0.20.6",
  "io.circe"      %% "circe-parser"        % "0.12.2",
  "io.circe"      %% "circe-derivation"    % "0.12.0-M7",
  compilerPlugin(("org.typelevel" %% "kind-projector" % "0.11.0").cross(CrossVersion.full))
)

Revolver.settings
enablePlugins(JavaAppPackaging)
