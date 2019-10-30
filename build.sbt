name := "hanamura"

version := "0.1"

description := "GraphQL server written with caliban - Hanamura."

scalaVersion := "2.12.10"

scalacOptions ++= Seq("-deprecation", "-feature")
scalacOptions ++= Seq("-Ypartial-unification")
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
updateOptions := updateOptions.value.withLatestSnapshots(false)
val akkaVersion = "2.5.19"
val akkaHttpVersion = "10.1.8"
val catsVersion = "2.0.0"

val catsDependencies = Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-free" % catsVersion
)

libraryDependencies ++= Seq(
  "io.nem" % "sdk-vertx-client" % "0.13.0-SNAPSHOT" changing(),
  "io.circe" %%	"circe-core" % "0.9.3",
  "io.circe" %% "circe-optics" % "0.9.3",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0",
  "com.typesafe" % "config"               % "1.3.3",
  "com.github.ghostdogpr" %% "caliban" % "0.1.1",
  "com.github.ghostdogpr" %% "caliban-http4s" % "0.1.1",
  "dev.zio"       %% "zio-interop-cats"    % "2.0.0.0-RC6",
  "org.typelevel" %% "cats-effect"         % "2.0.0",
  "org.http4s"    %% "http4s-dsl"          % "0.20.6",
  "org.http4s"    %% "http4s-circe"        % "0.20.6",
  "org.http4s"    %% "http4s-blaze-server" % "0.20.6",
  "io.circe"      %% "circe-parser"        % "0.12.2",
  "io.circe"      %% "circe-derivation"    % "0.12.0-M7",
  compilerPlugin(("org.typelevel" %% "kind-projector" % "0.11.0").cross(CrossVersion.full))
) ++ catsDependencies



Revolver.settings
enablePlugins(JavaAppPackaging)
