package com.tlohman.basicapp.hello

import org.http4s.{Header, Headers}
import org.typelevel.ci.CIString
import org.typelevel.otel4s.context.propagation.{TextMapGetter, TextMapUpdater}

package object otel {
  private[otel] implicit val headerGetter: TextMapGetter[Headers] =
    new TextMapGetter[Headers] {
      override def get(carrier: Headers, key: String): Option[String] =
        carrier
          .get(CIString(key))
          .map(_.head.value)

      override def keys(carrier: Headers): Iterable[String] =
        carrier
          .headers.view
          .map(_.name)
          .distinct
          .map(_.toString)
          .toSeq
    }

  private[otel] implicit val headerUpdater: TextMapUpdater[Headers] =
    new TextMapUpdater[Headers] {
      override def updated(carrier: Headers, key: String, value: String): Headers =
        carrier
          .put(Header.Raw(CIString(key), value))
    }
}
