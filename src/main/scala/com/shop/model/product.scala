package com.shop.model

import derevo.cats.{eqv, show}
import derevo.derive
import eu.timepit.refined.types.all.NonEmptyString
import io.estatico.newtype.macros.newtype
import squants.Money
import eu.timepit.refined.cats._


object product {
  @derive(eqv, show)
  @newtype case class ProductName(value: NonEmptyString)

  @derive(eqv, show)
  case class ShoppingProduct(name: ProductName, price: Money)
}