package com.shop.cart.http

import cats.effect.kernel.{Async, Resource}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

trait MkHttpClient[F[_]] {
  def newEmber: Resource[F, Client[F]]
}

object MkHttpClient {
  def apply[F[_]: MkHttpClient]: MkHttpClient[F] = implicitly

  implicit def forAsync[F[_]: Async]: MkHttpClient[F] =
    new MkHttpClient[F] {
      def newEmber: Resource[F, Client[F]] =
        EmberClientBuilder
          .default[F]
          .build
    }
}
