package com.tlohman.basicapp.hello.otel

import cats._
import cats.data._
import cats.syntax.all._
import cats.effect._
import cats.effect.implicits._
import cats.effect.std.Console
import org.http4s.client.RequestKey
import org.http4s.{Header, HttpApp, Request, Response, Uri}
import org.typelevel.otel4s.{Attribute, Attributes}
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.semconv.attributes
import org.typelevel.otel4s.semconv.attributes.{
  ClientAttributes,
  HttpAttributes,
  NetworkAttributes,
  ServerAttributes,
  UrlAttributes,
  UserAgentAttributes
}
import org.typelevel.otel4s.trace.{SpanKind, Tracer, StatusCode}
import smithy.api.Http
import smithy4s.{Hints, Service}
import smithy4s.http4s.ServerEndpointMiddleware
import smithy4s.schema.Schema
import smithy4s.hello.HelloWorldServiceGen.endpoint

object TraceContextMiddleware {
  def apply[F[_]: Sync: Console: Tracer] = new ServerEndpointMiddleware[F] {

    override def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
        endpoint: service.Endpoint[_, _, _, _, _]
    ): HttpApp[F] => HttpApp[F] = base =>
      HttpApp[F] { req =>
        MonadCancelThrow[F].uncancelable { poll =>
          Tracer[F].joinOrRoot(req.headers) {
            OptionT
              .fromOption[F](endpoint.hints.get[smithy.api.Http])
              .semiflatMap { hint =>
                Tracer[F]
                  .spanBuilder(serverSpanName(hint))
                  .withSpanKind(SpanKind.Server)
                  .addAttributes(requestAttributes(req, hint))
                  .build
                  .use { span =>
                    poll(base.run(req))
                      // base.run(req)
                      .guaranteeCase { outcome =>
                        (outcome match {
                          case Outcome.Succeeded(fa) =>
                            fa.flatMap { res =>
                              val out = responseAttributes(res)
                              span.addAttributes(out) >> span
                                .setStatus(
                                  StatusCode.Error
                                ) // TODO: Additional response attributes???
                                .unlessA(res.status.isSuccess)

                            }
                          case Outcome.Errored(e) =>
                            span.recordException(e)
                          case Outcome.Canceled() =>
                            Sync[F].unit
                        })
                      }
                  }
              }
              .getOrElseF {
                Tracer[F]
                  .spanBuilder(s"${req.method.name}")
                  .build
                  .use(_ => base(req))
              }
          }
        }
      }
  }

  private def serverSpanName[F[_]](hint: Http) =
    s"${hint.method.value} ${hint.uri.value}"

  private def requestAttributes[F[_]](
      request: Request[F],
      hint: Http
  ): Attributes = {
    def hostAndPort(host: String, mPort: Option[Int]): List[Attribute[_]] =
      ServerAttributes.ServerAddress(host) :: mPort
        .map(p => ServerAttributes.ServerPort(p))
        .toList

    import org.http4s.headers._
    Attributes(
      List(
        HttpAttributes.HttpRequestMethod(hint.method.value),
        HttpAttributes.HttpRoute(hint.uri.value)
      ) ++
        request.uri.scheme
          .map(sch => UrlAttributes.UrlScheme(sch.value))
          .toList ++
        request.headers
          .get[Forwarded]
          .flatMap(_.values.head.maybeHost)
          .map(h => hostAndPort(h.host.renderString, h.port))
          .orElse(
            request.headers.get[Host].map(h => hostAndPort(h.host, h.port))
          )
          .getOrElse {
            val auth = RequestKey
              .fromRequest(request)
              .authority
            hostAndPort(auth.host.renderString, auth.port)
          } ++
        request.headers
          .get[`User-Agent`]
          .map { ua =>
            UserAgentAttributes
              .UserAgentOriginal(`User-Agent`.headerInstance.value(ua))
          } ++
        request.headers
          .get[`X-Forwarded-For`]
          .map(xff => `X-Forwarded-For`.headerInstance.value(xff))
          .orElse(request.remoteAddr.map(_.toString))
          .map(c => ClientAttributes.ClientAddress(c)) ++
        request.remote.toList.flatMap { addr =>
          List(
            NetworkAttributes.NetworkPeerAddress(addr.host.toString),
            NetworkAttributes.NetworkPeerPort(addr.port.value)
          )
        }: _*
    )
  }

  private def responseAttributes[F[_]](response: Response[F]): Attributes = {
    val builder = Attributes.newBuilder

    builder += HttpAttributes.HttpResponseStatusCode(response.status.code)

    builder.result()
  }
}
