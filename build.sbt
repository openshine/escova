enablePlugins(GitVersioning)


lazy val esplugin = project
  .in(file("modules") / "escova-esplugin")
  .settings(common)
  .dependsOn(core)

lazy val core = project
  .in(file("modules") / "escova-core")
  .settings(common)

lazy val uservice = project
  .in(file("modules") / "escova-uservice")
  .settings(common)
  .dependsOn(core)

lazy val root = project.in(file("."))
  .settings(common)
  .settings(Seq(
    name := "escova"
  ))
  .settings(

    libraryDependencies ++= Seq(
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
    ),

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.4" % "test",
      "org.scalactic" %% "scalactic" % "3.0.4" % "test"
    )
  )


val common = Seq(
  organization := "com.openshine",
  scalaVersion := "2.12.4",
  git.useGitDescribe := true,
  git.baseVersion := "0.10-dev"
)

