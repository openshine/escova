name := "escova-akka-uservice"

enablePlugins(GitVersioning)

libraryDependencies ++= Seq(
  "org.elasticsearch" % "elasticsearch" % "5.6.3",
  "com.typesafe.akka" %% "akka-http" % "10.1.5",
  "com.typesafe" % "config" % "1.3.0",
  "com.github.pureconfig" %% "pureconfig" % "0.9.0"
  // Warning: Do not use Jackson due to dependency hell against
  // elasticsearch
  // as Elasticsearch does not include some jackson modules needed by
  // json4s.
  // If such is needed, require json4s-jackson with notTransitive(), but
  // that
  // should be avoided while possible.
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
  "org.scalactic" %% "scalactic" % "3.0.4" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.5" % Test
)
