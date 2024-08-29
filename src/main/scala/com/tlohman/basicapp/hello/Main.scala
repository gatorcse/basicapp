package com.tlohman.basicapp
package hello

import cats._
import cats.syntax.all._
import cats.effect._
import cats.effect.implicits._
import cats.effect.std.Console
import com.comcast.ip4s._
import com.tlohman.basicapp.hello.otel.TraceContextMiddleware
import io.opentelemetry.api.GlobalOpenTelemetry
import smithy4s.hello._
import org.http4s._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s.server.Router
import org.http4s.client.defaults
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import org.typelevel.log4cats.otel4s.TracedLoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.oteljava.context.Context
import org.typelevel.otel4s.trace.Tracer
import io.opentelemetry.api.{OpenTelemetry => JOpenTelemetry}
import io.opentelemetry.instrumentation
import smithy4s.http4s.SimpleRestJsonBuilder
import org.typelevel.otel4s.metrics.Meter
import io.opentelemetry.instrumentation.runtimemetrics.java17.RuntimeMetrics

class HelloImpl[F[_]: Monad: LoggerFactory] extends HelloWorldService[F] {
  private val logger: SelfAwareStructuredLogger[F] = LoggerFactory[F].getLogger

  override def hello(name: String, town: Option[String]): F[Greeting] = {
    town match {
      case None =>
        logger.info("No town given").as(Greeting(s"Hello, $name!"))
      case Some(t) =>
        logger.info("Have a town").as(Greeting(s"Hello $name from $t"))
    }
  }
}

object Routes {
  private def example[F[_]: Async: Console: Tracer: LoggerFactory]
      : Resource[F, HttpRoutes[F]] =
    SimpleRestJsonBuilder
      .routes(new HelloImpl[F])
      .middleware(TraceContextMiddleware[F])
      .resource

  private def docs[F[_]: Sync]: HttpRoutes[F] =
    smithy4s.http4s.swagger.docs[F](HelloWorldService)

  def all[F[_]: Async: Tracer: Console: LoggerFactory]
      : Resource[F, HttpRoutes[F]] =
    example[F].map(_ <+> docs[F])
}

object Main extends IOApp.Simple {
  private def registerRuntimeMetrics[F[_]: Sync](
      openTelemetry: JOpenTelemetry
  ): Resource[F, Unit] = {
    val acquire = Sync[F].delay(RuntimeMetrics.create(openTelemetry))

    Resource.make(acquire)(r => Sync[F].delay(r.close())).void
  }

  override val run: IO[Unit] = {
    val baseLogging: LoggerFactory[IO] = Slf4jFactory.create[IO]
    val bareLogger = baseLogging.getLogger
    bareLogger.info("Beginning application.") *>
      OtelJava
        .autoConfigured[IO]()
        .flatTap(otel4s => registerRuntimeMetrics(otel4s.underlying))
        .use { otel4s =>
          for {
            implicit0(tracer: Tracer[IO]) <- otel4s.tracerProvider.get(
              "com.tlohman.basicapp"
            )
            implicit0(meters: Meter[IO]) <- otel4s.meterProvider.get(
              "com.tlohman.basicapp"
            )
            implicit0(loggerFactory: LoggerFactory[IO]) = TracedLoggerFactory
              .traced(baseLogging)
            _ <- Routes
              .all[IO]
              .flatMap { routes =>
                EmberServerBuilder
                  .default[IO]
                  .withPort(port"8080")
                  .withHost(host"localhost")
                  .withHttpApp(routes.orNotFound)
                  .build
              }
              .evalTap(_ => bareLogger.info("Starting application."))
              .useForever
          } yield ()

        }
  }
}
