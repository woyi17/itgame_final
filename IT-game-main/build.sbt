lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    name := "ITSD Card Game 25-26",
    version := "1.1",
    scalaVersion := "2.13.1",
    // https://github.com/sbt/junit-interface
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
    libraryDependencies ++= Seq(
      guice,

      // Testing libraries for dealing with CompletionStage...
      "org.assertj" % "assertj-core" % "3.14.0" % Test,
      "org.awaitility" % "awaitility" % "4.0.1" % Test,
    ),
    libraryDependencies += "junit" % "junit" % "4.13.2",
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test exclude("junit", "junit-dep"),
    LessKeys.compress := true,
    PlayKeys.playInteractionMode := BlockingInteractionMode,
    javacOptions ++= Seq(
      "-Xlint:unchecked",
      "-Xlint:deprecation"
    )
  )
