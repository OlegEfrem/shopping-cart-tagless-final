package com.shop.repo

import com.shop.ShopCartError
import com.shop.model.cart.{Cart, CartId}

trait CartRepo[F[_]] {
  def createCart(): F[Cart]
  def readCart(cartId: CartId): F[Cart]
  def replaceCart(oldCart: Cart, newCart: Cart): F[Unit]
}

object error {
  abstract class CartRepoError(message: String, cause: Option[Exception] = None) extends ShopCartError(message, cause)

  case class CartNotFound(cartId: CartId) extends CartRepoError(s"Cart with id: $cartId not found.")
  case class CartModified(oldCart: Cart, modifiedCart: Cart) extends CartRepoError(s"Cart with id: ${oldCart.id} was already modified.")

}


