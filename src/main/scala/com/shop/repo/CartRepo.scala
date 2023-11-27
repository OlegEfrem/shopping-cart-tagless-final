package com.shop.repo

import cats.MonadThrow
import cats.effect.Ref
import cats.effect.std.UUIDGen
import com.shop.ShopCartError
import com.shop.config.Config.CartConfig
import com.shop.model.cart.{Cart, CartId}
import cats.implicits._
import com.shop.repo.error.{CartNotFound, ConcurrentCartModification}

trait CartRepo[F[_]] {
  def createCart(): F[Cart]

  def getCart(cartId: CartId): F[Cart]

  def replaceCart(oldCart: Cart, newCart: Cart): F[Cart]
}

object error {
  abstract class CartRepoError(message: String, cause: Option[Exception] = None) extends ShopCartError(message, cause)

  case class CartNotFound(cartId: CartId) extends CartRepoError(s"Cart with id: $cartId not found.")

  case class ConcurrentCartModification(oldCart: Cart, modifiedCart: Cart, newCart: Cart) extends CartRepoError(s"Cart with id: ${oldCart.id} was already modified.")

}

object CartRepo {
  def make[F[_] : MonadThrow : UUIDGen](cartsRef: Ref[F, Map[CartId, Cart]], config: CartConfig): CartRepo[F] =
    new CartRepo[F] {
      override def createCart(): F[Cart] = {
        for {
          uuid <- UUIDGen.randomUUID
          cart = Cart.empty(CartId(uuid), config.taxRate)
          _ <- cartsRef.update(_ + (cart.id -> cart))
        } yield cart
      }

      override def getCart(cartId: CartId): F[Cart] =
        for {
          carts <- cartsRef.get
          cart <- MonadThrow[F].fromOption(carts.get(cartId), CartNotFound(cartId))
        } yield cart

      override def replaceCart(oldCart: Cart, newCart: Cart): F[Cart] = {
        def concurrentModificationError(currentCart: Cart) = {
          ConcurrentCartModification(oldCart, currentCart, newCart)
        }

        for {
          updated <- cartsRef.tryModify { carts =>
            carts.get(oldCart.id) match {
              case Some(currentCart) if currentCart === oldCart =>
                val updatedCart = carts + (newCart.id -> newCart)
                (updatedCart, Right(newCart))
              case Some(currentCart) if currentCart =!= oldCart =>
                (carts, Left(concurrentModificationError(currentCart)))
              case None =>
                (carts, Left(CartNotFound(oldCart.id)))
            }
          }
          currentCart <- getCart(oldCart.id)
          updatedResult <- MonadThrow[F].fromOption(updated, concurrentModificationError(currentCart))
          modificationResult <- MonadThrow[F].fromEither(updatedResult)
        } yield modificationResult
      }

    }
}


