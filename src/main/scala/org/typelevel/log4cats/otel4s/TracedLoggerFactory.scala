package org.typelevel.log4cats
package otel4s

import cats._
import cats.syntax.all._
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import org.typelevel.otel4s.trace.Tracer

trait TracedLoggerFactory[F[_]] extends LoggerFactory[F] {

}

object TracedLoggerFactory extends LoggerFactoryGenCompanion {
  def apply[F[_]: TracedLoggerFactory]: TracedLoggerFactory[F] = implicitly

  def traced[F[_]: Monad : Tracer](base: LoggerFactory[F]): TracedLoggerFactory[F] = new TracedLoggerFactory[F] {

    override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
      new TracedLogger[F](base.getLoggerFromName(name))

    override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
      base.fromName(name).map(b => new TracedLogger[F](b))
  }
}

class TracedLogger[F[_]: Monad: Tracer](base: SelfAwareStructuredLogger[F]) extends SelfAwareStructuredLogger[F] {

  override def isTraceEnabled: F[Boolean] = base.isTraceEnabled

  override def isDebugEnabled: F[Boolean] = base.isDebugEnabled

  override def isInfoEnabled: F[Boolean] = base.isInfoEnabled

  override def isWarnEnabled: F[Boolean] = base.isWarnEnabled

  override def isErrorEnabled: F[Boolean] = base.isErrorEnabled

  private def enrich(ctx: Map[String, String]): F[Map[String, String]] =
    Tracer[F].currentSpanContext.map {
      case Some(spanContext) => ctx ++ Seq("TraceId" -> spanContext.traceIdHex, "SpanId" -> spanContext.spanIdHex)
      case None => ctx
    }

  override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
    enrich(ctx).flatMap { enrichedContext =>
      base.trace(enrichedContext)(msg)
    }

  override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    enrich(ctx).flatMap { enrichedContext =>
      base.trace(enrichedContext, t)(msg)
    }

  override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
    enrich(ctx).flatMap { enrichedContext =>
      base.debug(enrichedContext)(msg)
    }

  override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    enrich(ctx).flatMap { enrichedContext =>
      base.debug(enrichedContext, t)(msg)
    }

  override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
    enrich(ctx).flatMap { enrichedContext =>
      base.info(enrichedContext)(msg)
    }

  override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    enrich(ctx).flatMap { enrichedContext =>
      base.info(enrichedContext, t)(msg)
    }

  override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
    enrich(ctx).flatMap { enrichedContext =>
      base.warn(enrichedContext)(msg)
    }

  override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    enrich(ctx).flatMap { enrichedContext =>
      base.warn(enrichedContext, t)(msg)
    }

  override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
    enrich(ctx).flatMap { enrichedContext =>
      base.error(enrichedContext)(msg)
    }

  override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    enrich(ctx).flatMap { enrichedContext =>
      base.error(enrichedContext, t)(msg)
    }

  override def error(t: Throwable)(message: => String): F[Unit] =
    enrich(Map.empty).flatMap { enrichedContext =>
      base.error(enrichedContext, t)(message)
    }

  override def warn(t: Throwable)(message: => String): F[Unit] =
    enrich(Map.empty).flatMap { enrichedContext =>
      base.warn(enrichedContext, t)(message)
    }

  override def info(t: Throwable)(message: => String): F[Unit] =
    enrich(Map.empty).flatMap { enrichedContext =>
      base.info(enrichedContext, t)(message)
    }

  override def debug(t: Throwable)(message: => String): F[Unit] =
    enrich(Map.empty).flatMap { enrichedContext =>
      base.debug(enrichedContext, t)(message)
    }

  override def trace(t: Throwable)(message: => String): F[Unit] =
    enrich(Map.empty).flatMap { enrichedContext =>
      base.trace(enrichedContext, t)(message)
    }

  override def error(message: => String): F[Unit] =
    enrich(Map.empty).flatMap { enrichedContext =>
      base.error(enrichedContext)(message)
    }

  override def warn(message: => String): F[Unit] =
    enrich(Map.empty).flatMap { enrichedContext =>
      base.warn(enrichedContext)(message)
    }

  override def info(message: => String): F[Unit] =
    enrich(Map.empty).flatMap { enrichedContext =>
      base.info(enrichedContext)(message)
    }

  override def debug(message: => String): F[Unit] =
    enrich(Map.empty).flatMap { enrichedContext =>
      base.debug(enrichedContext)(message)
    }

  override def trace(message: => String): F[Unit] =
    enrich(Map.empty).flatMap { enrichedContext =>
      base.trace(enrichedContext)(message)
    }
}