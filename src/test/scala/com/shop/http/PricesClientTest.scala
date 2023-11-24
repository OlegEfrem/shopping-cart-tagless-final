package com.shop.http

import cats.effect.IO
import cats.implicits.catsSyntaxEq
import com.shop.generators._
import com.shop.model.error.ProductError
import com.shop.model.moneyContext
import com.shop.model.product.{ProductName, ShoppingProduct}
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Response}
import org.scalacheck.effect.PropF.forAllF
import squants.market.Money
import eu.timepit.refined.auto._

class PricesClientTest extends CatsEffectSuite with ScalaCheckEffectSuite {
  private val url = "http://localhost"

  private def routes(productName: String, response: IO[Response[IO]]) =
    HttpRoutes
      .of[IO] {
        case GET -> Root / s"${productName}.json" => response
      }
      .orNotFound

  test("HTTP-200 response") {
    forAllF { (productName: ProductName) =>
      val price = Money(1.12)
      val client = Client.fromHttpApp(routes(productName.value, Ok(itemPrice(productName, price.amount))))
      PricesClient
        .make[IO](client, url)
        .getPrice(productName)
        .map(product => assertEquals(product, ShoppingProduct(productName, price)))
    }
  }

  test("HTTP-404 response") {
    forAllF { (productName: ProductName) =>
      val client = Client.fromHttpApp(routes(s"${productName.value}", NotFound()))
      val expectedError = ProductError(productName, s"GET http://localhost/${productName.value}.json HTTP error, code:404, reason: Not Found")
      PricesClient
        .make[IO](client, url)
        .getPrice(productName)
        .attempt
        .map {
          case Left(e) => assertEquals(e, expectedError)
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
