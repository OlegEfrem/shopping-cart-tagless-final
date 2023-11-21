package com.shop.model

import com.shop.model.cart.CartId
import com.shop.model.product.ProductName

object error {
  case class ProductError(productName: ProductName, reason: String) extends Exception(s"Product with name: $productName, encountered error: $reason")

  case class CartError(cartId: CartId) extends Exception(s"Cart with id: $cartId not found.")
}