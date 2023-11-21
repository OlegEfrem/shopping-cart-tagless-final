package com.shop.service

import com.shop.model.cart.{Cart, CartId, Quantity}
import com.shop.model.product.ProductName

trait CartService[F[_]] {
  def createCart(): F[Cart]
  def addProduct(cartId: CartId, productName: ProductName, quantity: Quantity): F[Cart]
  def getCart(cartId: CartId): F[Cart]
}
