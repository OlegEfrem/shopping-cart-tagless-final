package com.shop.cart.config

import com.shop.cart.model.tax.TaxRate
import eu.timepit.refined.types.string.NonEmptyString
import squants.market.{Currency, GBP, USD}

import scala.math.BigDecimal.RoundingMode
import scala.math.BigDecimal.RoundingMode.RoundingMode

object Config {
  val defaultCurrency: Currency = USD
  val taxRate: TaxRate = TaxRate(12.5 / 100)
  object money {
    val defaultRoundingMode: RoundingMode = RoundingMode.HALF_UP
    val defaultScale = 2
  }
  val cartConfig: CartConfig = CartConfig(taxRate)
  val pricesClientConfig: PricesClientConfig = PricesClientConfig("https://equalexperts.github.io/backend-take-home-test-data/")
  case class CartConfig(taxRate: TaxRate)

  case class PricesClientConfig(url: String)
  case class MoneyConfig(scale: Int, roundingMode: RoundingMode)
}
