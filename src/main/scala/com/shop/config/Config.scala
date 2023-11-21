package com.shop.config

import com.shop.model.tax.TaxRate
import squants.market.{Currency, GBP}

object Config {
  val taxRate: TaxRate = TaxRate(12.5)
  val defaultCurrency: Currency = GBP
}
