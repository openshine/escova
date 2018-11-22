name := "escova-akka-uservice"

organization := "com.openshine"

organizationName := "openshine"

organizationHomepage := Some(url("http://www.openshine.com"))

enablePlugins(GitVersioning)
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

libraryDependencies ++= Seq(
  "org.elasticsearch" % "elasticsearch" % "5.6.3",
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe" % "config" % "1.3.0",
  "com.iheart" %% "ficus" % "1.4.3"
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

licenses := Seq(
  "Apache-2.0" -> url("http://www.apache.org/licenses-LICENSE-2.0.txt"),
  "MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")
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
