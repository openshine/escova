import java.io.{BufferedReader, FileReader, FileWriter, PrintWriter}

import sbt.Keys._
import sbt.{Def, _}

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

/**
  * This Sbt AutoPlugin creates the infrastructure needed to produce
  * an ElasticSearch plugin.
  *
  * The esplugin command is the main user-interaction entry point,
  * which will take zero or one parameter. If no parameters are
  * supplied, the plugin will be built against the version provided as
  * elasticsearchVersion in your project. If you want to provide the
  * plugin for several source-compatible versions you can recompile
  * the plugin for each one by providing an additional argument to the
  * esplugin command. Beware that when using from the command line,
  * you must call the command between quotes as a single argument to
  * the shell. For example, to build the plugin both against ES 5.3.6
  * and ES 5.5.0, you would use a command line such as:
  * {{{
  * $ sbt "esplugin 5.3.6" "esplugin 5.5.0"
  * }}}
  *
  * A description for the plugin must be input via
  * espluginDescription, and if there are newlines, they will be
  * properly converted to the Java-properties file. However, not much
  * testing was performed for this substitution, as it does not affect
  * so much the core functionality. If you are not ok with how the
  * substitutions are performed, please substitute the provided plugin
  * descriptor file with your own by setting espluginDescriptorFile
  * with a path to your own copy of the file.
  *
  * The plugin gets built by the espluginZip task, which should not be
  * overwritten unless its value needs changing. It will add the
  * appropriate version of the elasticsearch package as a provided
  * dependency, so you MUST NOT depend on ElasticSearch yourself in
  * your libraryDependencies.
  *
  * The projectSettings get augmented with default definitions,
  * including a default elasticsearchVersion and the plugin metadata
  * directory, in which to put processed files, such as the
  * plugin-security.policy file for files requiring
  * SecurityManager-related privileges.
  *
  * To the best of our knowledge, there was
  * no readily-available function in sbt to populate a Java-properties
  * file with variables (e.g., to substitute such variables by their
  * values) and that's why the Filter object exists. If that is
  * unnecessary, please send feedback.
  *
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object ElasticPlugin extends AutoPlugin with ElasticKeys {

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq[Def.Setting[_]](
    commands += esplugin,

    espluginMetadataDir := {
      baseDirectory.value / "src" / "main" / "plugin-metadata"
    },

    elasticsearchVersion := System
      .getProperty("elasticsearch.version", "5.6.3"),

    espluginJavaVersion := "1.8",

    espluginHasNativeController := false,

    espluginDescriptorFile := baseDirectory.value / "project" /
      "plugin-descriptor.properties",

    espluginZipBaseName := {
      val versionString = System.getProperty(
        "esplugin.zip.version.string",
        version.value)
      s"${name.value}-$versionString"
    },

    espluginZipName := {
      s"${espluginZipBaseName.value}-for-es-${elasticsearchVersion.value}.zip"
    },

    espluginZip := {
      val target = Keys.target.value
      val name = Keys.name.value
      val version = Keys.version.value

      val distdir: File = target / "elasticsearch"
      val zipFile: File = target / espluginZipName.value

      println(s"Using ElasticSearch version: ${elasticsearchVersion.value}")

      val allLibs: List[File] = dependencyClasspath
        .in(Runtime).value.map(_.data)
        .filter(_.isFile).toList

      val buildArtifact = packageBin.in(Runtime).value
      val jars: List[File] = buildArtifact :: allLibs

      val jarMappings = jars.map(f => (f, distdir / f.getName))
      val pluginMetadata = entries(espluginMetadataDir.value,
        includeDirs = false)
        .map(f => (f, distdir / f.getName))

      val metadataProps = Map(
        "description" -> espluginDescription.value,
        "version" -> version,
        "name" -> name,
        "classname" -> espluginClass.value,
        "javaVersion" -> espluginJavaVersion.value,
        "elasticsearchVersion" -> elasticsearchVersion.value,
        "hasNativeController" -> espluginHasNativeController.value.toString
      )

      IO.delete(zipFile)
      IO.delete(distdir)

      IO.createDirectory(distdir)
      IO.copy(jarMappings)
      IO.copy(pluginMetadata)

      Filter.apply(espluginDescriptorFile.value,
        distdir / espluginDescriptorFile.value.getName,
        metadataProps)


      IO.zip(entries(distdir).map(d =>
        (d, d.getAbsolutePath.substring(distdir.getParent.length + 1))),
        zipFile)
      zipFile
    },

    libraryDependencies ++= Seq(
      "org.elasticsearch" % "elasticsearch" % elasticsearchVersion
        .value % "provided"
    )
  )

  val esplugin = Command.args("esplugin", "<esversion>", Help(
    "esplugin", ("esplugin", "Build an ElasticSearch plugin"),
    """
      |Build a plugin for ElasticSearch, optionally providing a specific
      |version for compiling as an argument.
    """.stripMargin
  )) {
    (state, esv) =>
      implicit val extracted: Extracted = Project extract state
      val newState: State = extracted.append(
          Seq(
            elasticsearchVersion := esv.headOption
              .getOrElse(elasticsearchVersion.value)
          ),
        state)

      val (s, _) = Project.extract(newState)
        .runTask(espluginZip in Compile, newState)
      s
  }

  override def requires = plugins.JvmPlugin

  // Do not enable automatically
  override def trigger = noTrigger

  private def entries(f: File, includeDirs: Boolean = true): List[File] =
    if (f.isDirectory) {
      val r = IO.listFiles(f).toList.flatMap(entries(_, includeDirs))
      if (includeDirs)
        f :: r
      else r
    } else List(f)

  object Filter {
    private val pattern = """((?:\\?)\$\{.+?\})""".r

    def apply(src: File, dst: File, props: Map[String, String])
    : Unit = {
      val in = new BufferedReader(new FileReader(src))
      val out = new PrintWriter(new FileWriter(dst))
      IO.foreachLine(in) { line =>
        IO.writeLines(out, Seq(filter(line, props)))
      }
      in.close()
      out.close()
    }

    private def filter(line: String, props: Map[String, String]) = {
      pattern.replaceSomeIn(line, replacer(props))
    }

    private def replacer(props: Map[String, String]) = (m: Match) => {
      m.matched match {
        case s if s.startsWith("\\") => Some(
          """\$\{%s\}""" format s.substring
          (3, s.length - 1))
        case s => props.get(s.substring(2, s.length - 1)).map(multilineFormat)
      }
    }

    private def multilineFormat(in: String): String = {
      in.replaceAll("\\n", Regex.quoteReplacement("\\\\n\\\\\n"))
    }
  }

  object autoImport extends ElasticKeys

}
