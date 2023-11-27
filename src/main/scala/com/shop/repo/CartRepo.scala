package com.shop.repo

import cats.MonadThrow
import cats.effect.Concurrent
import cats.effect.std.{MapRef, UUIDGen}
import cats.implicits._
import com.shop.ShopCartError
import com.shop.config.Config.CartConfig
import com.shop.model.cart.{Cart, CartId}
import com.shop.repo.error.{CartNotFound, CartToModifyChanged, DifferentCartsReplacement}

trait CartRepo[F[_]] {
  def createCart(): F[Cart]

  def getCart(cartId: CartId): F[Cart]

  def replaceCart(oldCart: Cart, newCart: Cart): F[Cart]
}

object error {
  abstract class CartRepoError(message: String, cause: Option[Exception] = None) extends ShopCartError(message, cause)

  case class CartNotFound(cartId: CartId) extends CartRepoError(s"Cart with id: $cartId not found.")

  case class CartToModifyChanged(oldCart: Cart, modifiedCart: Cart, newCart: Cart) extends CartRepoError(s"Cart to modify: ${oldCart.id} was changed.")

  case class DifferentCartsReplacement(oldCart: Cart, newCart: Cart) extends CartRepoError(s"Tried to replace cart with id: ${oldCart.id} with cart with id: ${newCart.id}.")

}

object CartRepo {
  def make[F[_] : MonadThrow : Concurrent : UUIDGen](cartsRef: MapRef[F, CartId, Option[Cart]], config: CartConfig): CartRepo[F] =
    new CartRepo[F] {
      override def createCart(): F[Cart] = {
        for {
          uuid <- UUIDGen.randomUUID
          cart = Cart.empty(CartId(uuid), config.taxRate)
          _ <- cartsRef(cart.id).update(_ => cart.some)
        } yield cart
      }

      override def getCart(cartId: CartId): F[Cart] = {
        for {
          maybeCart <- cartsRef(cartId).get
          cartOrNotFound <- MonadThrow[F].fromOption(maybeCart, CartNotFound(cartId))
        } yield cartOrNotFound
      }

      override def replaceCart(oldCart: Cart, newCart: Cart): F[Cart] = {

        val cartsIdValidation =
          if (oldCart.id =!= newCart.id)
            Left(DifferentCartsReplacement(oldCart, newCart))
          else
            Right(())
        for {
          _ <- MonadThrow[F].fromEither(cartsIdValidation)
          updatedCartOrError <- cartsRef(oldCart.id).modify {
            case Some(currentCart) if currentCart === oldCart =>
              (newCart.some, Right(newCart))
            case Some(currentCart) if currentCart =!= oldCart =>
              (oldCart.some, Left(CartToModifyChanged(oldCart, currentCart, newCart)))
            case None | Some(_) =>
              (oldCart.some, Left(CartNotFound(oldCart.id)))
          }
          modificationResult <- MonadThrow[F].fromEither(updatedCartOrError)
        } yield modificationResult
      }

    }
}


