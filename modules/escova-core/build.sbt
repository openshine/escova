name := "escova-core"

organization := "com.openshine"

organizationName := "openshine"

organizationHomepage := Some(url("http://www.openshine.com"))

enablePlugins(GitVersioning)

libraryDependencies ++= Seq(
  "org.elasticsearch" % "elasticsearch" % "5.6.3" % "provided",
  "org.json4s" %% "json4s-ast" % "3.5.3",
  "org.json4s" %% "json4s-core" % "3.5.3",
  "org.json4s" %% "json4s-native" % "3.5.3"
  // Warning: Do not use Jackson due to dependency hell against
  // elasticsearch
  // as Elasticsearch does not include some jackson modules needed by
  // json4s.
  // If such is needed, require json4s-jackson with notTransitive(), but
  // that
  // should be avoided while possible.
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "org.scalactic" %% "scalactic" % "3.0.4" % "test"
)

licenses := Seq(
  "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
)

developers := List(
  Developer("ssaavedra",
            "Santiago Saavedra",
            "@ssaavedra",
            url("https://github.com/ssaavedra"))
)

scmInfo := Some(
  ScmInfo(url("https://github.com/openshine/escova"),
          "git@github.com:openshine/escova.git")
)
