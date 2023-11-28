package com.shop.config

import com.shop.config.Config.{MoneyConfig, defaultCurrency}
import squants.market.MoneyContext

object Implicits {
  implicit val moneyContext: MoneyContext = MoneyContext(defaultCurrency, Set.empty, Seq.empty)
  implicit val moneyConfig: MoneyConfig = MoneyConfig(Config.money.defaultScale, Config.money.defaultRoundingMode)
}
