package com.shop.repo

import cats.effect.{IO, Ref}
import com.shop.TestData._
import com.shop.config.Config.CartConfig
import com.shop.generators._
import com.shop.model.cart.{Cart, CartId}
import com.shop.repo.error.CartNotFound
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.effect.PropF.forAllF
import cats.syntax.all._

import java.util.UUID

class CartRepoTest extends CatsEffectSuite with ScalaCheckEffectSuite {

  private def refAndRepo(carts: Map[CartId, Cart] = Map.empty): IO[(Ref[IO, Map[CartId, Cart]], CartRepo[IO])] = {
    for {
      carts <- Ref.of[IO, Map[CartId, Cart]](carts)
    } yield (carts, CartRepo.make[IO](carts, CartConfig()))

  }

  test("Creates a new empty cart returning it") {
    for {
      (cartsRef, cartsRepo) <- refAndRepo()
      newCart <- cartsRepo.createCart()
      refCarts <- cartsRef.get
    } yield assertEquals(refCarts(newCart.id), newCart)
  }

  test("Gets an existing cart") {
    forAllF { (cart: Cart) =>
      for {
        (cartsRef, cartsRepo) <- refAndRepo(Map(cart.id -> cart))
        obtainedCart <- cartsRepo.getCart(cart.id)
        refCarts <- cartsRef.get
      } yield assertEquals(refCarts(obtainedCart.id), obtainedCart)
    }
  }

  test(s"Returns error on attempt to get a non existing cart") {
    val cartId = aCartId
    interceptMessageIO[CartNotFound](s"Cart with id: $cartId not found.") {
      for {
        (_, cartsRepo) <- refAndRepo()
        _ <- cartsRepo.getCart(cartId)
      } yield ()
    }
  }

  test("Replaces an existing cart with the new one") {
    forAllF { (cart: Cart) =>
      val oldCart = anEmptyCart(cart.id)
      for {
        (cartsRef, cartsRepo) <- refAndRepo(Map(oldCart.id -> oldCart))
        _ <- cartsRepo.replaceCart(oldCart, cart)
        refCarts <- cartsRef.get
      } yield assertEquals(refCarts(cart.id), cart)
    }
  }

  test("Returns error on same cart concurrent modification") {
    forAllF { (cart: Cart) =>
      val oldCart = anEmptyCart(cart.id)
      interceptMessageIO[CartNotFound](s"Cart with id: ${cart.id} was already modified.") {
        for {
          (_, cartsRepo) <- refAndRepo(Map(cart.id -> cart))
          _ <- cartsRepo.replaceCart(oldCart, cart)
        } yield ()
      }
      assertIO(IO.pure(1), 1)
    }
  }


  test("Replaces an existing cart on different carts concurrent modification") {
    forAllF { (cart: Cart) =>
      def cartId(i: Int): CartId = CartId(UUID.nameUUIDFromBytes(i.toString.getBytes()))

      val carts = (1 to 10).map(i => anEmptyCart(cartId(i)) -> cart.copy(id = cartId(i))).toMap
      val emptyCarts = carts.keys.map(ec => ec.id -> ec).toMap
      for {
        (cartsRef, cartsRepo) <- refAndRepo(emptyCarts)
        _ <- carts.map { case (empty, nonEmpty) => cartsRepo.replaceCart(empty, nonEmpty) }.toList.parSequence.void
        refCarts <- cartsRef.get
      } yield assertEquals(refCarts.size, 100)
    }
  }


}
