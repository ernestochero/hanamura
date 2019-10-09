name := "hanamura"

version := "0.1"

scalaVersion := "2.13.1"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "io.nem" % "sdk-vertx-client" % "0.13.0-SNAPSHOT" changing()