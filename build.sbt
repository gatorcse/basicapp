import smithy4s.codegen.Smithy4sCodegenPlugin

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val http4sVersion = "0.23.27"

lazy val smithy4sOtel4sMiddlware =
  (project in file("modules/smithy4s-otel4s-middleware"))
    .settings(
      name := "smithy4s-otel4s-middlware",
      addCompilerPlugin(
        "org.typelevel" % "kind-projector" % "0.13.3" cross CrossVersion.full
      ),
      addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % "2.12.0",
        "org.typelevel" %% "cats-effect" % "3.5.4",
        "org.http4s" %% "http4s-dsl" % http4sVersion,
        "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
        "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
        "org.typelevel" %% "otel4s-oteljava" % "0.8.1" excludeAll (
          ExclusionRule(
            organization = "io.opentelemetry",
            name = "opentelemetry-sdk"
          ),
          ExclusionRule(
            organization = "io.opentelemetry",
            name = "opentelemetry-sdk-extension-autoconfigure"
          )
        ),
        "org.typelevel" %% "otel4s-semconv" % "0.8.0"
      )
    )

lazy val basicapp = (project in file("testbed/basicapp"))
  .enablePlugins(Smithy4sCodegenPlugin, JavaAgent)
  .dependsOn(smithy4sOtel4sMiddlware)
  .settings(
    name := "basicapp",
    addCompilerPlugin(
      "org.typelevel" % "kind-projector" % "0.13.3" cross CrossVersion.full
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.12.0",
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "org.apache.logging.log4j" % "log4j-slf4j2-impl" % "2.23.1",
      "org.apache.logging.log4j" % "log4j-api" % "2.23.1",
      "org.apache.logging.log4j" % "log4j-core" % "2.23.1",
      "org.typelevel" %% "otel4s-oteljava" % "0.8.1" excludeAll (
        ExclusionRule(
          organization = "io.opentelemetry",
          name = "opentelemetry-sdk"
        ),
        ExclusionRule(
          organization = "io.opentelemetry",
          name = "opentelemetry-sdk-extension-autoconfigure"
        )
      ),
      "org.typelevel" %% "otel4s-semconv" % "0.8.0"
    ),
    javaAgents += "com.datadoghq" % "dd-java-agent" % "1.38.1" % "runtime;dist;compile",
//    javaOptions += "-Dotel.java.global-autoconfigure.enabled=true",
//    javaOptions += "-Dotel.service.name=basicapp",
//    javaOptions += "-Dotel.exporter.otlp.endpoint=http://localhost:4317",
    // javaOptions += "dd.trace.otel.enabled=true",
    javaOptions += "-Ddd.profiling.enabled=true",
    javaOptions += "-XX:FlightRecorderOptions=stackdepth=256",
    javaOptions += "-Ddd.logs.injection=false",
    javaOptions += "-Ddd.service=basicapp",
    javaOptions += "-Ddd.trace.otel.enabled=true",
    javaOptions += "-Ddd.trace.propagation.style=tracecontext",
    javaOptions += "-Ddd.env=staging",
    javaOptions += "-Ddd.version=1.0",
    githubWorkflowPublishPreamble := Seq()
  )

lazy val root = project.aggregate(smithy4sOtel4sMiddlware, basicapp)
