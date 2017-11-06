name := "escova"

organization := "com.openshine"

// version := "0.1.0"

scalaVersion := "2.12.4"

enablePlugins(GitVersioning)
git.useGitDescribe := true
git.baseVersion := "0.1.0"

espluginClass := "com.openshine.escova.esplugin.EscovaPlugin"
espluginDescription :=
  """
    |ESCOVA is a Cost Analyzer and Validation Assistant for ES Queries
  """.stripMargin.trim


libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-ast" % "3.5.3",
  "org.json4s" %% "json4s-core" % "3.5.3",
  "org.json4s" %% "json4s-native" % "3.5.3"
  // Warning: Do not use Jackson due to dependency hell against elasticsearch
  // as Elasticsearch does not include some jackson modules needed by json4s.
  // If such is needed, require json4s-jackson with notTransitive(), but that
  // should be avoided while possible.
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "org.scalactic" %% "scalactic" % "3.0.4" % "test"
)
