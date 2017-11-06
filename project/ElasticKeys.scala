import sbt._

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
trait ElasticKeys {
  lazy val espluginZip = taskKey[File]("Creates an Elasticsearch plugin")
  lazy val espluginMetadataDir = settingKey[File]("Plugin metadata directory")
  lazy val elasticsearchVersion = settingKey[String]("The version of ES this " +
    "plugin is built for")

  lazy val espluginJavaVersion = settingKey[String](
    """
      |The Java version of this project.
      |Must match the Java version for the selected elasticsearchVersion.
    """.stripMargin
  )

  lazy val espluginHasNativeController = settingKey[Boolean]("Whether the " +
    "plugin has a native controller")

  lazy val espluginDescription = settingKey[String]("Plugin description")
  lazy val espluginClass = settingKey[String](
    "Which is the main class for the plugin")

}

object ElasticKeys extends ElasticKeys
