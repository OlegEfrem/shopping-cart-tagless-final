package com.shop

import cats.{Eq, Monoid, Show}
import com.shop.config.Config.money.{defaultRoundingMode, defaultScale}
import com.shop.config.Implicits.moneyContext
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.types.all.PosInt
import io.circe.{Decoder, Encoder}
import squants.market.{Currency, Money}

package object model extends OrphanInstances

trait OrphanInstances {
  implicit val moneyDecoder: Decoder[Money] =
    Decoder[BigDecimal].map(Money.apply)

  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  implicit val moneyMonoid: Monoid[Money] =
    new Monoid[Money] {
      def empty: Money = Money(0)

      def combine(x: Money, y: Money): Money = x + y
    }

  implicit val posIntMonoid: Monoid[PosInt] =
    new Monoid[PosInt] {
      def empty: PosInt = 1

      def combine(x: PosInt, y: PosInt): PosInt = Refined.unsafeApply(x + y)
    }

  implicit val currencyEq: Eq[Currency] = Eq.and(Eq.and(Eq.by(_.code), Eq.by(_.symbol)), Eq.by(_.name))

  implicit val moneyEq: Eq[Money] = Eq.and(Eq.by(_.amount), Eq.by(_.currency))

  implicit val moneyShow: Show[Money] = Show.show(m => m.rounded(defaultScale, defaultRoundingMode).toString)

}

