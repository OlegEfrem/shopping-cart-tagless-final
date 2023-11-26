package com.shop

import com.shop.TestData.productPrices
import com.shop.config.Config.taxRate
import com.shop.model.cart._
import com.shop.model.moneyContext
import com.shop.model.product.{ProductName, ShoppingProduct}
import com.shop.model.tax.Tax
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import org.scalacheck.{Arbitrary, Gen}
import squants.Money
import squants.market.Money

import java.util.UUID
import scala.language.implicitConversions


object generators {

  private val nonEmptyStringGen: Gen[String] =
    Gen
      .chooseNum(21, 40)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }
  implicit val nonEmptyStringArb: Arbitrary[String] = Arbitrary(nonEmptyStringGen)

  private def nesGen[A](f: String => A): Gen[A] =
    nonEmptyStringGen.map(f)

  private def idGen[A](f: UUID => A): Gen[A] = Gen.uuid.map(f)

  private val productNameGen: Gen[ProductName] = Gen.oneOf(
    Seq(
      ProductName("cheerios"),
      ProductName("cornflakes"),
      ProductName("frosties"),
      ProductName("shreddies"),
      ProductName("weetabix")
    )
  )
  implicit val productNameArb: Arbitrary[ProductName] = Arbitrary(productNameGen)

  private val cartIdGen: Gen[CartId] = idGen(CartId.apply)
  implicit val cartIdArb: Arbitrary[CartId] = Arbitrary(cartIdGen)

  private val quantityGen: Gen[Quantity] = Gen.chooseNum(1, 5).map(n => Quantity(Refined.unsafeApply(n)))
  implicit val quantityArb: Arbitrary[Quantity] = Arbitrary(quantityGen)

  private val moneyGen: Gen[Money] = Gen.chooseNum[BigDecimal](1, 10).map(n => Money(n)(model.moneyContext))
  implicit val moneyArb: Arbitrary[Money] = Arbitrary(moneyGen)

  private val productGen: Gen[ShoppingProduct] = for {
    prodName <- productNameGen
  } yield ShoppingProduct(prodName, productPrices(prodName))
  implicit val productArb: Arbitrary[ShoppingProduct] = Arbitrary(productGen)

  private val itemGen: Gen[Item] = for {
    prod <- productGen
    quantity <- quantityGen
  } yield Item(prod, quantity)
  implicit val itemArb: Arbitrary[Item] = Arbitrary(itemGen)

  private val itemsMapGen: Gen[(ProductName, Item)] = {
    for {
      item <- itemGen
    } yield item.product.name -> item
  }

  private val itemsGen: Gen[Map[ProductName, Item]] = for {
    prodNums <- Gen.chooseNum(2, 5)
    prods <- Gen.mapOfN(prodNums, itemsMapGen)
  } yield prods

  private val cartGen: Gen[Cart] = for {
    id <- cartIdGen
    items <- itemsGen
    subTotal = subTotalFor(items)
    taxAmount = subTotal * taxRate.value
    total = subTotal + taxAmount
    cartTotals = CartTotals(subTotal = subTotal, tax = Tax(taxRate, taxAmount), total = total)
  } yield Cart(id, items, cartTotals)
  implicit val cartArb: Arbitrary[Cart] = Arbitrary(cartGen)

  private def subTotalFor(items: Map[ProductName, Item]): Money = {
    items.foldLeft(MoneyOps.zero) { case (acc, (_, item)) => acc + item.product.price * item.quantity.value.value }
  }

}
