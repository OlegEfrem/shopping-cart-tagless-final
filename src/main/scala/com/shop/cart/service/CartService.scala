package com.shop.cart
package service

import cats.MonadThrow
import cats.implicits._
import com.shop.cart.config.Config.CartConfig
import com.shop.cart.error.CartError
import com.shop.cart.http.PricesClient
import com.shop.cart.model._
import com.shop.cart.model.cart._
import com.shop.cart.model.product.{ProductName, ShoppingProduct}
import com.shop.cart.model.tax.Tax
import com.shop.cart.repo.CartRepo

trait CartService[F[_]] {
  def createCart(): F[Cart]
  def getCart(cartId: CartId): F[Cart]
  def addProduct(cartId: CartId, productName: ProductName, quantity: Quantity): F[Cart]

}

object error {

  abstract class CartServiceError(override val message: String, cause: Option[Exception] = None) extends CartError(message, cause)

}

object CartService {
  def make[F[_]: MonadThrow](
      pricesClient: PricesClient[F],
      cartRepo: CartRepo[F],
      cartConfig: CartConfig
  ): CartService[F] = new CartService[F] {

    override def createCart(): F[Cart] = cartRepo.createCart()

    override def getCart(cartId: CartId): F[Cart] = cartRepo.getCart(cartId)

    override def addProduct(cartId: CartId, productName: ProductName, quantity: Quantity): F[Cart] = {
      for {
        oldCart <- cartRepo.getCart(cartId)
        newPrice <- pricesClient.getPrice(productName)
        newCart = addProduct(oldCart, ShoppingProduct(productName, newPrice), quantity)
      } yield newCart
    }

    private def addProduct(oldCart: Cart, product: ShoppingProduct, quantity: Quantity): Cart = {
      val newItem = addQuantity(oldCart, product, quantity)
      val newItems = oldCart.items.+(product.name -> newItem)
      val newTotals = calculateTotals(newItems.values.toList)
      Cart(oldCart.id, newItems, newTotals)
    }

    private def addQuantity(oldCart: Cart, product: ShoppingProduct, quantity: Quantity): Item = {
      val newQuantity = oldCart.items.get(product.name) match {
        case Some(item) => item.quantity |+| quantity
        case None       => quantity
      }
      Item(product, newQuantity)
    }

    private def calculateTotals(items: List[Item]): CartTotals = {
      val subTotal = items.foldLeft(MoneyOps.zero) { case (acc, item) => acc |+| item.product.price * item.quantity.value.value }
      val taxAmount = subTotal * cartConfig.taxRate.value
      val total = subTotal + taxAmount
      CartTotals(subTotal, Tax(cartConfig.taxRate, taxAmount), total)
    }
  }
}
