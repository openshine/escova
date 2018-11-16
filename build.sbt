enablePlugins(GitVersioning)

lazy val esplugin = project
  .in(file("modules") / "escova-esplugin")
  .settings(common)
  .settings(
    Seq(
      // Our descriptor file is in the root project's directory
      espluginDescriptorFile := baseDirectory.value /
        ".." / ".." / "project" / "plugin-descriptor.properties"
    ))
  .dependsOn(core)
  .aggregate(core)

lazy val core = project
  .in(file("modules") / "escova-core")
  .settings(common)
  .settings(Seq(exportJars := true))

lazy val uservice = project
  .in(file("modules") / "escova-uservice")
  .settings(common)
  .dependsOn(core)

lazy val root = project
  .in(file("."))
  .settings(
    Seq(
      name := "escova"
    ))
  .aggregate(uservice, esplugin)

run in Compile ~= (old => run in Compile in uservice)

val common = Seq(
  organization := "com.openshine",
  scalaVersion := "2.12.4",
  git.useGitDescribe := true,
  git.baseVersion := "0.10-dev"
)
