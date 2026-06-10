import org.typelevel.scalacoptions.ScalacOption
import sbt.Keys.libraryDependencies
import sbt.Def
import Dependencies.*
import com.repcheck.sbt.ExceptionUniquenessPlugin.autoImport.exceptionUniquenessRootPackages

val isScala212: Def.Initialize[Boolean] = Def.setting {
  VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector("2.12.x"))
}

ThisBuild / dynverSonatypeSnapshots := true

lazy val commonSettings = Seq(
  organization := "com.repcheck",
  scalaVersion := "3.7.3",
  publishTo := Some(
    "GitHub Packages" at s"https://maven.pkg.github.com/Eligio-Taveras/repcheck-utils"
  ),
  publishMavenStyle := true,
  credentials ++= {
    val envCreds = for {
      user  <- sys.env.get("GITHUB_ACTOR")
      token <- sys.env.get("GITHUB_TOKEN")
    } yield Credentials("GitHub Package Registry", "maven.pkg.github.com", user, token)

    val fileCreds = {
      val f = Path.userHome / ".sbt" / ".github-packages-credentials"
      if (f.exists) Some(Credentials(f)) else None
    }

    envCreds.orElse(fileCreds).toSeq
  },
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.18" % Test
  ),
  semanticdbEnabled := true,
  tpolecatScalacOptions ++= ScalaCConfig.scalaCOptions,
  tpolecatScalacOptions ++= {
    if (isScala212.value) ScalaCConfig.scalaCOption2_12
    else Set.empty[ScalacOption]
  },

  // WartRemover — enforces FP discipline at compile time
  wartremoverErrors ++= Seq(
    Wart.AsInstanceOf,          // No unsafe casts
    Wart.EitherProjectionPartial, // No .get on Either projections
    Wart.IsInstanceOf,          // No runtime type checks — use pattern matching
    Wart.MutableDataStructures, // No mutable collections
    Wart.Null,                  // No null — use Option
    Wart.OptionPartial,         // No Option.get — use fold/map/getOrElse
    Wart.Return,                // No return statements
    Wart.StringPlusAny,         // No string + any — use interpolation
    Wart.IterableOps,           // No .head/.tail on collections — use headOption
    Wart.TryPartial,            // No Try.get — use fold/recover
    Wart.Var                    // No mutable vars
  ),
  wartremoverWarnings ++= Seq(
    Wart.Throw                  // Warn on bare throw — prefer F.raiseError
  )
)

lazy val root = (project in file("."))
  .aggregate(repcheckutils, utilsDoobie, docGenerator)
  .settings(
    commonSettings,
    name := "repcheck-utils-root",
    publish / skip := true
  )

lazy val repcheckutils = (project in file("repcheck-utils"))
  .enablePlugins(com.repcheck.sbt.ExceptionUniquenessPlugin)
  .settings(
    commonSettings,
    name := "repcheck-utils",
    // Foundation library: no repcheck deps, no HTTP/streaming — only effects, codecs, config
    libraryDependencies ++= circe ++ pureConfig ++ catsEffect ++ testDeps,
    // Provided: tags/E2ETest extends org.scalatest.Tag; consumers always have scalatest on their test classpath
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18" % Provided,
    // Suppress Scala 3 ScalaTest-matcher warnings in TEST sources only (mirrors llm-adapter / shared-models)
    Test / scalacOptions += "-Wconf:msg=unused value of type:s",
    Test / scalacOptions += "-Wconf:msg=is not declared infix:s",
    // Coverage gate: every file must be >= 95% statement AND branch coverage or CI fails
    coverageMinimumStmtPerFile   := 95,
    coverageMinimumBranchPerFile := 95,
    coverageFailOnMinimum         := true,
    exceptionUniquenessRootPackages := Seq("com.repcheck")
  )

lazy val utilsDoobie = (project in file("utils-doobie"))
  .enablePlugins(com.repcheck.sbt.ExceptionUniquenessPlugin)
  .settings(
    commonSettings,
    name := "repcheck-utils-doobie",
    // Doobie/Postgres codecs split from the core so consumers that don't touch a database stay doobie-free
    libraryDependencies ++= circe ++ doobie ++ testDeps,
    Test / scalacOptions += "-Wconf:msg=unused value of type:s",
    Test / scalacOptions += "-Wconf:msg=is not declared infix:s",
    coverageMinimumStmtPerFile   := 95,
    coverageMinimumBranchPerFile := 95,
    coverageFailOnMinimum         := true,
    exceptionUniquenessRootPackages := Seq("com.repcheck")
  )

lazy val docGenerator = (project in file("doc-generator"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.anthropic" % "anthropic-java" % "2.18.0",
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "ch.qos.logback" % "logback-classic" % "1.5.6"
    ),
    // Exclude WartRemover for this utility project — uses Java SDK patterns
    wartremoverErrors := Seq.empty,
    wartremoverWarnings := Seq.empty,
    // Exclude from coverage — utility project with no unit tests
    coverageEnabled := false
  )
