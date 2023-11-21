package com.shop

import cats.{Eq, Monoid, Show}
import com.shop.config.Config.defaultCurrency
import io.circe.{Decoder, Encoder}
import squants.market.{Currency, GBP, Money, MoneyContext}

package object model extends OrphanInstances {
  implicit val moneyContext: MoneyContext = MoneyContext(defaultCurrency, Set.empty, Seq.empty)
}

trait OrphanInstances {
  implicit val moneyDecoder: Decoder[Money] =
    Decoder[BigDecimal].map(GBP.apply)

  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  implicit val moneyMonoid: Monoid[Money] =
    new Monoid[Money] {
      def empty: Money = GBP(0)

      def combine(x: Money, y: Money): Money = x + y
    }

  implicit val currencyEq: Eq[Currency] = Eq.and(Eq.and(Eq.by(_.code), Eq.by(_.symbol)), Eq.by(_.name))

  implicit val moneyEq: Eq[Money] = Eq.and(Eq.by(_.amount), Eq.by(_.currency))

  implicit val moneyShow: Show[Money] = Show.fromToString

}

