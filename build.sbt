name := "elasticsearch-complexity-analyzer"

version := "1.0"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "org.elasticsearch" % "elasticsearch" % "5.6.3",

  // Network stack
  "io.netty" % "netty-buffer" % "4.1.13.Final",
  "io.netty" % "netty-codec" % "4.1.13.Final",
  "io.netty" % "netty-codec-http" % "4.1.13.Final",
  "io.netty" % "netty-common" % "4.1.13.Final",
  "io.netty" % "netty-handler" % "4.1.13.Final",
  "io.netty" % "netty-resolver" % "4.1.13.Final",
  "io.netty" % "netty-transport" % "4.1.13.Final"
)

