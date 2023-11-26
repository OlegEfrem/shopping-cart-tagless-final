package com.shop.model

import com.shop.config.Config.MoneyConfig
import com.shop.model.product.{ProductName, ShoppingProduct}
import com.shop.model.tax.{Tax, TaxRate}
import derevo.cats.{eqv, monoid, show}
import derevo.derive
import eu.timepit.refined.types.all.PosInt
import io.estatico.newtype.macros.newtype
import squants.Money
import squants.market.Money

import java.util.UUID
import eu.timepit.refined.cats._
import eu.timepit.refined.api.Refined

object cart {
  @derive(eqv, monoid, show)
  @newtype case class Quantity(value: PosInt)

  @derive(eqv, show)
  case class Item(product: ShoppingProduct, quantity: Quantity)

  @derive(eqv, show)
  @newtype case class CartId(value: UUID)

  @derive(eqv, show)
  case class CartTotals(subTotal: Money, tax: Tax, total: Money) {
    def rounded(implicit moneyConfig: MoneyConfig): CartTotals = CartTotals(subTotal.round, Tax(tax.rate, tax.amount.round), total.round)
    private implicit class MoneyOps(money: Money) {
      def round(implicit config: MoneyConfig): Money = money.rounded(config.scale, config.roundingMode)
    }

  }

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