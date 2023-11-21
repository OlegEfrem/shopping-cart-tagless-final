package com.shop.model

import derevo.cats.{eqv, show}
import derevo.derive
import io.estatico.newtype.macros.newtype
import squants.Money


object product {
  @derive(eqv, show)
  @newtype case class ProductName(value: String)

  @derive(eqv, show)
  case class ShoppingProduct(name: ProductName, price: Money)
}