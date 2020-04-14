name := "hanamura"

version := "0.1"

description := "GraphQL server written with caliban - Hanamura."

scalaVersion := "2.12.10"

scalacOptions ++= Seq("-deprecation", "-feature")
scalacOptions ++= Seq("-Ypartial-unification")
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
updateOptions := updateOptions.value.withLatestSnapshots(false)
val calibanVersion = "0.7.4"
libraryDependencies ++= Seq(
  "io.nem" % "sdk-vertx-client" % "0.16.2" changing(),
  "io.circe" %%	"circe-core" % "0.9.3",
  "io.circe" %% "circe-optics" % "0.9.3",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0",
  "com.typesafe" % "config"               % "1.3.3",
  "com.github.ghostdogpr" %% "caliban" % calibanVersion,
  "com.github.ghostdogpr" %% "caliban-http4s" % calibanVersion,
  "io.circe"      %% "circe-derivation"    % "0.12.0-M7",
  "com.github.pureconfig" %% "pureconfig" % "0.12.1",
  compilerPlugin(("org.typelevel" %% "kind-projector" % "0.11.0").cross(CrossVersion.full))
)

Revolver.settings
enablePlugins(JavaAppPackaging)
