package com.shop.config

import com.shop.model.tax.TaxRate
import squants.market.{Currency, GBP}

import scala.math.BigDecimal.RoundingMode
import scala.math.BigDecimal.RoundingMode.RoundingMode

object Config {
  val defaultCurrency: Currency = GBP
  val taxRate: TaxRate = TaxRate(12.5 / 100)
  object money {
    val defaultRoundingMode: RoundingMode = RoundingMode.HALF_UP
    val defaultScale = 2
  }
  case class CartConfig(taxRate: TaxRate = taxRate)
  case class MoneyConfig(scale: Int, roundingMode: RoundingMode)
}
