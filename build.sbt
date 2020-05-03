name := "hanamura"

version := "0.1"

description := "GraphQL server written with caliban - Hanamura."

scalaVersion := "2.12.10"

scalacOptions ++= Seq("-deprecation", "-feature")
scalacOptions ++= Seq("-Ypartial-unification")
// resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
updateOptions := updateOptions.value.withLatestSnapshots(false)
val calibanVersion = "0.7.4"
val circeVersion = "0.13.0"
val tsecVersion = "0.2.0"
val calibanDependencies = Seq(
  "com.github.ghostdogpr" %% "caliban" % calibanVersion,
  "com.github.ghostdogpr" %% "caliban-http4s" % calibanVersion
)
val circeDependencies = Seq(
  "io.circe" %%	"circe-core" % circeVersion,
  "io.circe" %% "circe-optics" % circeVersion,
  "io.circe"      %% "circe-derivation"    % "0.12.0-M7",
)

val loggerDependencies = Seq(
  "org.log4s" %% "log4s" % "1.8.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
)

val tsecDependencies = Seq(
  "io.github.jmcardon" %% "tsec-common" % tsecVersion,
  "io.github.jmcardon" %% "tsec-cipher-core" % tsecVersion,
  "io.github.jmcardon" %% "tsec-cipher-jca" % tsecVersion,
  "io.github.jmcardon" %% "tsec-hash-jca" % tsecVersion,
)

libraryDependencies ++= Seq(
  "io.nem" % "symbol-sdk-vertx-client" % "0.17.1",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0",
  "com.typesafe" % "config"               % "1.3.3",
  "com.github.pureconfig" %% "pureconfig" % "0.12.1",
  compilerPlugin(("org.typelevel" %% "kind-projector" % "0.11.0").cross(CrossVersion.full))
) ++ calibanDependencies ++ circeDependencies ++ loggerDependencies ++ tsecDependencies

Revolver.settings
enablePlugins(JavaAppPackaging)
