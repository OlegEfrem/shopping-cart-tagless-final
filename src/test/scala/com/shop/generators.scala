package com.shop

import com.shop.model.cart.Quantity
import com.shop.model.product.ProductName
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.scalacheck.{Arbitrary, Gen}
import squants.Money
import squants.market.Money


object generators {

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

  private val quantityGen: Gen[Quantity] = Gen.posNum[Int].map(n => Quantity(Refined.unsafeApply(n)))

  private val moneyGen: Gen[Money] = Gen.posNum[Int].map(n => Money(n)(model.moneyContext))

  implicit val arbQuantity: Arbitrary[Quantity] = Arbitrary(quantityGen)

  implicit val arbMoney: Arbitrary[Money] = Arbitrary(moneyGen)

  implicit val arbNonEmptyString: Arbitrary[String] =
    Arbitrary(Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString))

/*  implicit def coercibleArbitrary[A: Coercible[B, *], B: Arbitrary]: Arbitrary[A] =
    Arbitrary(Arbitrary.arbitrary[B].map(_.coerce[A]))*/
}
