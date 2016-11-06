import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtGhPages._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import com.typesafe.sbt.SbtGit.{GitKeys => git}
import com.typesafe.sbt.SbtSite._
import com.typesafe.sbt.SbtSite.SiteKeys.siteMappings
import scoverage.ScoverageKeys.coverageEnabled
import sbt.LocalProject
import sbt.Tests.{InProcess, Group}

object Resolvers {
  val typesafe = Seq(
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
  )
  val resolversList = typesafe
}

object Dependencies {
  val akkaVersion = Def.setting {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n <= 11 =>
        "2.3.6"
      case _ =>
        "2.4.12"
    }
  }

  import sbt._

  val akkaActor = Def.setting("com.typesafe.akka" %% "akka-actor" % akkaVersion.value)

  val akkaTestkit = Def.setting("com.typesafe.akka" %% "akka-testkit" % akkaVersion.value)

  val specs2 = "org.specs2" %% "specs2-core" % "3.8.6"

  val stm = "org.scala-stm" %% "scala-stm" % "0.7"

  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.4"

  //val scalameter = "com.github.axel22" %% "scalameter" % "0.4"

  val rediscalaDependencies = Def.setting(Seq(
    akkaActor.value,
    akkaTestkit.value % "test",
    //scalameter % "test",
    specs2 % "test",
    scalacheck % "test"
  ))
}

object RediscalaBuild extends Build {
  val baseSourceUrl = "https://github.com/etaty/rediscala/tree/"


  lazy val standardSettings = Defaults.defaultSettings ++
    Seq(
      name := "rediscala",
      organization := "com.github.etaty",
      scalaVersion := "2.11.7",
      crossScalaVersions := Seq("2.11.7", "2.10.4"),
      licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
      homepage := Some(url("https://github.com/non/rediscala")),
      scmInfo := Some(ScmInfo(url("https://github.com/etaty/rediscala"), "scm:git:git@github.com:etaty/rediscala.git")),
      apiURL := Some(url("http://etaty.github.io/rediscala/latest/api/")),
      pomExtra := (
        <developers>
          <developer>
            <id>etaty</id>
            <name>Valerian Barbot</name>
            <url>http://github.com/etaty/</url>
          </developer>
        </developers>
        ),
      resolvers ++= Resolvers.resolversList,

      publishMavenStyle := true,
      git.gitRemoteRepo := "git@github.com:etaty/rediscala.git",

      // TODO https://github.com/scoverage/scalac-scoverage-plugin/issues/192
      coverageEnabled := {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, n)) if n >= 12 => false
          case _ => coverageEnabled.value
        }
      },

      scalacOptions ++= Seq(
        "-encoding", "UTF-8",
        "-Xlint",
        "-deprecation",
        "-Xfatal-warnings",
        "-feature",
        "-language:postfixOps",
        "-unchecked"
      ),
      scalacOptions in (Compile, doc) <++= baseDirectory in LocalProject("rediscala") map { bd =>
        Seq(
          "-sourcepath", bd.getAbsolutePath
        )
      },
      autoAPIMappings := true,
      apiURL := Some(url("http://etaty.github.io/rediscala/")),
      scalacOptions in (Compile, doc) <++= version in LocalProject("rediscala") map { version =>
        val branch = if(version.trim.endsWith("SNAPSHOT")) "master" else version
        Seq[String](
          "-doc-source-url", baseSourceUrl + branch +"€{FILE_PATH}.scala"
        )
      }
  ) ++ site.settings ++ ghpages.settings ++ Seq(
      siteMappings <++= (mappings in packageDoc in Compile, version in LocalProject("rediscala")) { (mm, version) =>
        mm.map{ m =>
          for((f, d) <- m) yield (f, version + "/api/" + d)
        }
      },
      cleanSite <<= (updatedRepository, git.gitRunner, streams, version in LocalProject("rediscala")) map { (dir, git, s, v) =>
        val toClean = IO.listFiles(dir).filter{ f =>
          val p = f.getName
          p.startsWith("latest") || p.startsWith(v)
        }.map(_.getAbsolutePath).toList
        if(toClean.nonEmpty)
          git(("rm" :: "-r" :: "-f" :: "--ignore-unmatch" :: toClean) :_*)(dir, s.log)
        ()
      },
      synchLocal <<= (cleanSite, privateMappings, updatedRepository, ghpagesNoJekyll, git.gitRunner, streams) map { (clean, mappings, repo, noJekyll, git, s) =>
        // TODO - an sbt.Synch with cache of previous mappings to make this more efficient. */
        val betterMappings = mappings map { case (file, target) => (file, repo / target) }
        // First, remove 'stale' files.
        //cleanSite.
        // Now copy files.
        IO.copy(betterMappings)
        if(noJekyll) IO.touch(repo / ".nojekyll")
        repo
      }
  ) ++ site.includeScaladoc("latest/api")

  lazy val BenchTest = config("bench") extend Test

  lazy val benchTestSettings = inConfig(BenchTest)(Defaults.testSettings ++ Seq(
    sourceDirectory in BenchTest <<= baseDirectory / "src/benchmark",
    //testOptions in BenchTest += Tests.Argument("-preJDK7"),
    testFrameworks in BenchTest := Seq(new TestFramework("org.scalameter.ScalaMeterFramework")),

    //https://github.com/sbt/sbt/issues/539 => bug fixed in sbt 0.13.x
    testGrouping in BenchTest <<= definedTests in BenchTest map partitionTests
  ))

  lazy val root = Project(id = "rediscala",
    base = file("."),
    settings = standardSettings ++ Seq(
      libraryDependencies ++= Dependencies.rediscalaDependencies.value
    )
  ).configs(BenchTest)
    //.settings(benchTestSettings: _* )

  lazy val benchmark = {
    import pl.project13.scala.sbt.JmhPlugin

    Project(
      id = "benchmark",
      base = file("benchmark")
    ).settings(Seq(
      scalaVersion := "2.11.7",
      libraryDependencies += "net.debasishg" %% "redisclient" % "3.0"
    ))
      .enablePlugins(JmhPlugin)
      .dependsOn(root)
  }

  def partitionTests(tests: Seq[TestDefinition]) = {
    Seq(new Group("inProcess", tests, InProcess))
  }
}
