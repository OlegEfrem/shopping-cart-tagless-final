package com.shop.cart

import cats.effect.std.MapRef
import cats.effect.{IO, IOApp, Resource}
import com.shop.cart.config.Config
import com.shop.cart.http.{MkHttpClient, PricesClient}
import com.shop.cart.model.cart.{Cart, CartId, Quantity}
import com.shop.cart.model.product.ProductName
import com.shop.cart.repo.CartRepo
import com.shop.cart.service.CartService
import com.shop.cart.visualization.Visualizations._
import eu.timepit.refined.auto._

object Main extends IOApp.Simple {
  def run: IO[Unit] = mkCartService().use { cartService =>
    for {
      cart <- cartService.createCart()
      _ <- cartService.addProduct(cart.id, ProductName("cornflakes"), Quantity(1))
      _ <- cartService.addProduct(cart.id, ProductName("cornflakes"), Quantity(1))
      _ <- cartService.addProduct(cart.id, ProductName("weetabix"), Quantity(1))
      savedCart <- cartService.getCart(cart.id)
      _ <- IO.println(savedCart.prettyPrint)
    } yield ()
  }

  private def mkCartService(): Resource[IO, CartService[IO]] =
    for {
      httpClient <- MkHttpClient[IO].newEmber
      cartsRef <- Resource.eval(MapRef.ofShardedImmutableMap[IO, CartId, Cart](1))
      pricesClientConfig = Config.pricesClientConfig
      pricesClient = PricesClient.make[IO](httpClient, pricesClientConfig)
      cartConfig = Config.cartConfig
      cartRepo = CartRepo.make[IO](cartsRef, cartConfig)
      cartService = CartService.make[IO](pricesClient, cartRepo, cartConfig)
    } yield cartService
}
