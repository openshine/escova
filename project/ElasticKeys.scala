import sbt._

/**
  * These are the keys used in the plugin and which can be customized
  * by the respective build.sbt on usage.
  *
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
trait ElasticKeys {

  /**
    * This task creates the ElasticSearch Plugin and returns the zip
    * file containing it.
    */
  lazy val espluginZip = taskKey[File]("Creates an Elasticsearch plugin")

  lazy val espluginZipBaseName =
    settingKey[String]("Returns the ElasticSearch plugin name")

  lazy val espluginZipName = settingKey[String](
    "Returns the ElasticSearch " +
      "plugin name. Overide this variable to change the zip name")

  lazy val espluginDescriptorFile = settingKey[File](
    "The plugin description " +
      "file. You usually need not change this value, and instead fill the " +
      "other esplugin values")

  /**
    * The metadata directory where your security policy would be. If
    * you don't need a security policy, please leave the folder
    * empty. The default directory matches the recommendations by
    * elasticsearch of having a "plugin-metadata" directory in your src/main.
    */
  lazy val espluginMetadataDir = settingKey[File]("Plugin metadata directory")

  /**
    * The main version of Elasticsearch you want your plugin to work
    * against. This will be the version run by default and when
    * running the tests.
    */
  lazy val elasticsearchVersion = settingKey[String](
    "The version of ES this " +
      "plugin is built for")

  /**
    * This should be set to "1.8" but it is available here for future
    * proofing the mechanism whenever we switch to Java 9.
    */
  lazy val espluginJavaVersion = settingKey[String](
    """
      |The Java version of this project.
      |Must match the Java version for the selected elasticsearchVersion.
    """.stripMargin
  )

  lazy val espluginHasNativeController = settingKey[Boolean](
    "Whether the " +
      "plugin has a native controller")

  /**
    * The description of this plugin. This will be injected in the
    * Java properties file. Beware of special characters and escaping
    * characters. If you need something intrincate, you may want to
    * take a look at the code and probably even run some tests.
    */
  lazy val espluginDescription = settingKey[String]("Plugin description")

  /**
    * The class in the project implementing the ES plugin
    * interface. This is a String because we do not have access to the
    * compile code yet here, thus we cannot make it typesafe.
    */
  lazy val espluginClass =
    settingKey[String]("Which is the main class for the plugin")

}

object ElasticKeys extends ElasticKeys
