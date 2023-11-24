package com.shop
package http

import cats.effect.MonadCancelThrow
import cats.implicits._
import com.shop.model.error.ProductError
import com.shop.model.product.{ProductName, ShoppingProduct}
import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import eu.timepit.refined.types.all.NonEmptyString
import org.http4s.Method.GET
import org.http4s.circe.{JsonDecoder, toMessageSyntax}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{Status, Uri}
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import io.circe.refined._
import squants.market.Money
import model.moneyContext

trait PricesClient[F[_]] {
  def getPrice(productName: ProductName): F[ShoppingProduct]
}

object PricesClient {
  @derive(eqv, show, decoder, encoder)
  case class ItemPrice(title: NonEmptyString, price: BigDecimal)

  def make[F[_] : JsonDecoder : MonadCancelThrow](
                                                   client: Client[F],
                                                   url: String
                                                 ): PricesClient[F] = {
    new PricesClient[F] with Http4sClientDsl[F] {
      override def getPrice(productName: ProductName): F[ShoppingProduct] =
        Uri.fromString(url + s"/${productName.value}.json").liftTo[F].flatMap { uri =>
          val request = GET(uri)
          client.run(request).use { resp =>
            resp.status match {
              case Status.Ok =>
                resp.asJsonDecode[ItemPrice].map(i => ShoppingProduct(productName, Money(i.price)))
              case st =>
                val msg = Option(st.reason).getOrElse("unknown")
                val errorMsg = s"${request.method} ${request.uri} HTTP error, code:${st.code}, reason: $msg"
                ProductError(productName, errorMsg)
                  .raiseError[F, ShoppingProduct]
            }
          }
        }
    }

  }
}
