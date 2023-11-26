package com.shop

import com.shop.model.moneyContext
import com.shop.model.product.ProductName
import eu.timepit.refined.auto._
import squants.market.Money

object TestData {
  val cheerios: ProductName = ProductName("cheerios")
  val cornflakes: ProductName = ProductName("cornflakes")
  val frosties: ProductName = ProductName("frosties")
  val shreddies: ProductName = ProductName("shreddies")
  val weetabix: ProductName = ProductName("weetabix")

  val productPrices: Map[ProductName, Money] = Map(
    cheerios -> Money(8.43),
    cornflakes -> Money(2.52),
    frosties -> Money(4.99),
    shreddies -> Money(4.68),
    weetabix -> Money(9.98)
  )
}
