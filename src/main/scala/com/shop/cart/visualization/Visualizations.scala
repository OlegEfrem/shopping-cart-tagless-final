package com.shop.cart.visualization

import com.shop.cart.config.Implicits.moneyConfig
import com.shop.cart.model.cart.Cart

object Visualizations {
  implicit class CartOps(cart: Cart) {
    def prettyPrint: String = {
      import cart._
      val added = items.values
        .map { item =>
          s"Cart contains ${item.quantity} × ${item.product.name} @ ${item.product.price} each"
        }
        .mkString("\n")
      val roundedTotals = totals.rounded
      s"""
         |$added
         |Subtotal = ${roundedTotals.subtotal.toFormattedString}
         |Tax = ${roundedTotals.tax.amount.toFormattedString}
         |Total = ${roundedTotals.total.toFormattedString}
         |""".stripMargin
    }
  }
}
