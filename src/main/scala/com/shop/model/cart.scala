package com.shop.model

import com.shop.model.product.{ProductName, ShoppingProduct}
import com.shop.model.tax.{Tax, TaxRate}
import derevo.cats.{eqv, monoid, show}
import derevo.derive
import io.estatico.newtype.macros.newtype
import squants.Money
import squants.market.Money

import java.util.UUID

object cart {
  @derive(eqv, monoid, show)
  @newtype case class Quantity(value: Int)

  @derive(eqv, show)
  case class Item(product: ShoppingProduct, quantity: Quantity)

  @derive(eqv, show)
  @newtype case class CartId(value: UUID)

  @derive(eqv, show)
  case class CartTotals(subTotal: Money, tax: Tax, total: Money)

  object CartTotals {
    def zero(taxRate: TaxRate): CartTotals = CartTotals(MoneyOps.zero, Tax(taxRate, MoneyOps.zero), MoneyOps.zero)
  }

  @derive(eqv, show)
  case class Cart(id: CartId, items: Map[ProductName, Item], totals: CartTotals)

  object Cart {
    def empty(id: CartId, taxRate: TaxRate): Cart = Cart(id, Map.empty, CartTotals.zero(taxRate))
  }

  object MoneyOps {
    val zero: Money = Money(0)
  }

}