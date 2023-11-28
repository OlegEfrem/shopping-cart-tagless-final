package com.shop.cart.model

import derevo.cats.{eqv, monoid, show}
import derevo.derive
import io.estatico.newtype.macros.newtype
import squants.market.Money

object tax {
  @derive(eqv, monoid, show)
  @newtype case class TaxRate(value: BigDecimal)

  @derive(eqv, show)
  case class Tax(rate: TaxRate, amount: Money)
}