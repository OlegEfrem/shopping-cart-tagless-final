package com.shop.repo

import cats.effect.IO
import cats.effect.std.MapRef
import cats.implicits._
import com.shop.TestData._
import com.shop.config.Config.CartConfig
import com.shop.generators._
import com.shop.model.cart.{Cart, CartId}
import com.shop.repo.error.{CartNotFound, CartToModifyChanged, DifferentCartsReplacement}
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.effect.PropF.forAllF

import java.util.UUID

class CartRepoTest extends CatsEffectSuite with ScalaCheckEffectSuite {

  private def refAndRepo(carts: Map[CartId, Cart] = Map.empty, shardCount: Int = 10): IO[(MapRef[IO, CartId, Option[Cart]], CartRepo[IO])] = {
    for {
      cartsRef <- MapRef.ofShardedImmutableMap[IO, CartId, Cart](shardCount)
      _ <- carts.map { case (key, value) => cartsRef(key).update(_ => value.some) }.toList.sequence
    } yield (cartsRef, CartRepo.make[IO](cartsRef, CartConfig()))

  }

  test("Creates a new empty") {
    for {
      (cartsRef, cartsRepo) <- refAndRepo()
      createdCart <- cartsRepo.createCart()
      refCart <- cartsRef(createdCart.id).get
    } yield assertEquals(refCart, createdCart.some)
  }

  test("Gets an existing cart") {
    forAllF { (cart: Cart) =>
      for {
        (cartsRef, cartsRepo) <- refAndRepo(Map(cart.id -> cart))
        createdCart <- cartsRepo.getCart(cart.id)
        refCart <- cartsRef(createdCart.id).get
      } yield assertEquals(refCart, createdCart.some)
    }
  }

  test(s"Returns error if cart not found") {
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
        refCart <- cartsRef(cart.id).get
      } yield assertEquals(refCart, cart.some)
    }
  }

  test("Returns error on different carts replacement") {
    forAllF { (cart: Cart) =>
      val oldCart = anEmptyCart()
      interceptMessageIO[DifferentCartsReplacement](s"Tried to replace cart with id: ${oldCart.id} with cart with id: ${cart.id}.") {
        for {
          (_, cartsRepo) <- refAndRepo(Map(cart.id -> cart))
          _ <- cartsRepo.replaceCart(oldCart, cart)
        } yield ()
      }
      assertIO(IO.pure(1), 1)
    }
  }

  test("Returns error on same cart concurrent modification") {
    forAllF { (cart: Cart) =>
      val oldCart = anEmptyCart(cart.id)
      interceptMessageIO[CartToModifyChanged](s"Cart to modify:: ${cart.id} was changed.") {
        for {
          (_, cartsRepo) <- refAndRepo(Map(cart.id -> cart))
          _ <- cartsRepo.replaceCart(oldCart, cart)
        } yield ()
      }
      assertIO(IO.pure(1), 1)
    }
  }


  test("Replaces different carts concurrently") {
    forAllF { (cart: Cart) =>
      def cartId(i: Int): CartId = CartId(UUID.nameUUIDFromBytes(i.toString.getBytes()))
      val cartsSize = 100
      val cartIds = (1 to cartsSize).map(cartId)
      val carts = cartIds.map(i => anEmptyCart(i) -> cart.copy(id = i)).toMap
      val emptyCarts = carts.keys.map(ec => ec.id -> ec).toMap
      for {
        (cartsRef, cartsRepo) <- refAndRepo(emptyCarts)
        _ <- carts.map { case (empty, nonEmpty) => cartsRepo.replaceCart(empty, nonEmpty) }.toList.parSequence.void
        refCarts <- cartIds.map(cartsRef(_).get).toList.sequence
      } yield assertEquals(refCarts.size, cartsSize)
    }
  }


}
