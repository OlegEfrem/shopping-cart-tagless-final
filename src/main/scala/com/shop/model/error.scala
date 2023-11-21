package com.shop.model

import com.shop.model.cart.CartId
import com.shop.model.product.ProductName

object error {
  case class ProductError(productName: ProductName, reason: String) extends Exception(s"Product: $productName, error: $reason")

  case class CartError(cartId: CartId, reason: String) extends Exception(s"Cart id: $cartId, error: $reason")
}