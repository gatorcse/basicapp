package org.typelevel.otel4s.oteljava

import cats.Applicative
import cats.effect.Async
import cats.effect.Resource
import cats.effect.Sync
import cats.syntax.all._
import io.opentelemetry.api.{OpenTelemetry => JOpenTelemetry}
import io.opentelemetry.api.GlobalOpenTelemetry
import org.typelevel.otel4s.Otel4s
import org.typelevel.otel4s.context.LocalProvider
import org.typelevel.otel4s.context.propagation.ContextPropagators
import org.typelevel.otel4s.metrics.MeterProvider
import org.typelevel.otel4s.oteljava.context.Context
import org.typelevel.otel4s.oteljava.context.LocalContext
import org.typelevel.otel4s.oteljava.context.LocalContextProvider
import org.typelevel.otel4s.oteljava.context.propagation.PropagatorConverters._
import org.typelevel.otel4s.oteljava.metrics.Metrics
import org.typelevel.otel4s.oteljava.trace.Traces
import org.typelevel.otel4s.trace.TracerProvider

final class OtelJavaNoSDK[F[_]] private (
  val underlying: JOpenTelemetry,
  val propagators: ContextPropagators[Context],
  val meterProvider: MeterProvider[F],
  val tracerProvider: TracerProvider[F],
)(implicit val localContext: LocalContext[F])
  extends Otel4s[F] {
  override type Ctx = Context

  override def toString: String = s"OtelJavaNoSDK${underlying}"
}

object OtelJavaNoSDK {
  /** Creates an [[org.typelevel.otel4s.Otel4s]] from a Java OpenTelemetry
   * instance.
   *
   * @param jOtel
   *   A Java OpenTelemetry instance. It is the caller's responsibility to shut
   *   this down. Failure to do so may result in lost metrics and traces.
   *
   * @return
   *   An effect of an [[org.typelevel.otel4s.Otel4s]] resource.
   */
  def forAsync[F[_]: Async: LocalContextProvider](
    jOtel: JOpenTelemetry
  ): F[OtelJavaNoSDK[F]] =
    LocalProvider[F, Context].local.map { implicit l =>
      local[F](jOtel)
    }

  def local[F[_]: Async: LocalContext](
    jOtel: JOpenTelemetry
  ): OtelJavaNoSDK[F] = {
    val contextPropagators = jOtel.getPropagators.asScala

    val metrics = Metrics.forAsync(jOtel)
    val traces = Traces.local(jOtel, contextPropagators)
    new OtelJavaNoSDK[F](
      jOtel,
      contextPropagators,
      metrics.meterProvider,
      traces.tracerProvider,
    )
  }

  /** Creates a no-op implementation of the [[OtelJava]].
   */
  def noop[F[_]: Applicative: LocalContextProvider]: F[OtelJavaNoSDK[F]] =
    for {
      local <- LocalProvider[F, Context].local
    } yield new OtelJavaNoSDK(
      JOpenTelemetry.noop(),
      ContextPropagators.noop,
      MeterProvider.noop,
      TracerProvider.noop
    )(local)

  /** Lifts the acquisition of a Java OpenTelemetrySdk instance to a Resource.
   *
   * @param acquire
   *   OpenTelemetrySdk resource
   *
   * @return
   *   An [[org.typelevel.otel4s.Otel4s]] resource.
   */
  def resource[F[_]: Async: LocalContextProvider, A <: JOpenTelemetry](
    acquire: F[A],
  )(
      shutdown: A => F[Unit]
  ): Resource[F, OtelJavaNoSDK[F]] =
    Resource
      .make(acquire)(shutdown)
      .evalMap(forAsync[F])

  /** Creates an [[org.typelevel.otel4s.Otel4s]] from the global Java
   * OpenTelemetry instance.
   *
   * @see
   *   [[autoConfigured]]
   */
  def global[F[_]: Async: LocalContextProvider]: F[OtelJavaNoSDK[F]] =
    Sync[F].delay(GlobalOpenTelemetry.get).flatMap(forAsync[F])
}