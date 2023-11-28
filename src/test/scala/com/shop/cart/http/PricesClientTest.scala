package com.shop.cart.http

import cats.effect.IO
import com.shop.cart.config.Implicits.moneyContext
import com.shop.cart.generators._
import com.shop.cart.http.error.PricesClientError
import com.shop.cart.model.product.ProductName
import eu.timepit.refined.auto._
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Response}
import org.scalacheck.effect.PropF.forAllF
import squants.market.Money

class PricesClientTest extends CatsEffectSuite with ScalaCheckEffectSuite {
  private val url = "http://localhost"

  private def routes(productName: String, response: IO[Response[IO]]) =
    HttpRoutes
      .of[IO] { case GET -> Root / s"${productName}.json" =>
        response
      }
      .orNotFound

  test("HTTP-200 response") {
    forAllF { (productName: ProductName) =>
      val expectedPrice = Money(1.12)
      val client = Client.fromHttpApp(routes(productName.value, Ok(itemPrice(productName, expectedPrice.amount))))
      PricesClient
        .make[IO](client, url)
        .getPrice(productName)
        .map(obtainedPrice => assertEquals(obtainedPrice, expectedPrice))
    }
  }

  test("HTTP-404 response") {
    forAllF { (productName: ProductName) =>
      val client = Client.fromHttpApp(routes(s"${productName.value}", NotFound()))
      val expectedError = PricesClientError(productName, s"GET http://localhost/${productName.value}.json HTTP error, code:404, reason: Not Found")
      PricesClient
        .make[IO](client, url)
        .getPrice(productName)
        .attempt
        .map {
          case Left(e)  => assertEquals(e, expectedError)
          case Right(_) => fail("expected test failure")
        }
    }
  }

  private def itemPrice(productName: ProductName, price: BigDecimal) =
    s"""{
       |"title":"${productName.value}",
       |"price":"$price"
       |}""".stripMargin
}
