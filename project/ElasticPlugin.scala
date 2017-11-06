import java.io.{BufferedReader, FileReader, FileWriter, PrintWriter}

import sbt.Keys._
import sbt.{Def, _}

import scala.util.matching.Regex.Match

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object ElasticPlugin extends AutoPlugin with ElasticKeys {

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq[Def.Setting[_]](
    commands += esplugin,

    espluginMetadataDir := {
      baseDirectory.value / "src" / "main" / "plugin-metadata"
    },

    elasticsearchVersion := "5.6.3",

    espluginJavaVersion := "1.8",

    espluginHasNativeController := false,

    espluginZip := {
      val target = Keys.target.value
      val name = Keys.name.value
      val version = Keys.version.value

      val distdir: File = target / "elasticsearch"
      val zipFile: File = target /
        s"$name-$version-for-es-${elasticsearchVersion.value}.zip"

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

      val pluginDescriptorFile = baseDirectory.value / "project" /
        "plugin-descriptor.properties"

      IO.delete(zipFile)
      IO.delete(distdir)

      IO.createDirectory(distdir)
      IO.copy(jarMappings)
      IO.copy(pluginMetadata)

      Filter.apply(pluginDescriptorFile,
        distdir / pluginDescriptorFile.getName,
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
      val extracted = Project extract state
      val newState = extracted.append(Seq(
        elasticsearchVersion := esv.headOption
          .getOrElse(elasticsearchVersion.value)),
        state)
      val (s, _) = Project.extract(newState).runTask(espluginZip in Compile,
        newState)
      s
  }

  override def requires = plugins.JvmPlugin

  override def trigger = allRequirements

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
        case s => props.get(s.substring(2, s.length - 1))
      }
    }
  }

  object autoImport extends ElasticKeys

}
