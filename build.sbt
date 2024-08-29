import smithy4s.codegen.Smithy4sCodegenPlugin

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val http4sVersion = "0.23.27"

lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
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
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "net.logstash.logback" % "logstash-logback-encoder" % "8.0",
      "io.opentelemetry.instrumentation" % "opentelemetry-logback-appender-1.0" % "2.5.0-alpha",
      "org.typelevel" %% "otel4s-oteljava" % "0.8.1",
      "org.typelevel" %% "otel4s-semconv" % "0.8.0",
      "io.opentelemetry" % "opentelemetry-exporter-otlp" % "1.41.0" % Runtime,
      "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % "1.41.0" % Runtime,
      "io.opentelemetry.instrumentation" % "opentelemetry-runtime-telemetry-java17" % "2.5.0-alpha"
    ),
    javaOptions += "-Dotel.java.global-autoconfigure.enabled=true",
    javaOptions += "-Dotel.service.name=basicapp",
    javaOptions += "-Dotel.exporter.otlp.endpoint=http://localhost:4317",
    javaOptions += "dd.trace.otel.enabled=true",
    githubWorkflowPublishPreamble := Seq()
  )
