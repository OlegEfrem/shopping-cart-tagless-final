package com.shop

import munit.CatsEffectSuite
import squants.market.{Currency, GBP, Money, MoneyContext}

import java.util.UUID

class CartServiceTest extends CatsEffectSuite {
  implicit val mc: MoneyContext = MoneyContext(GBP, Set.empty, Seq.empty)
  private val cartService = ???
  private val cheeriosProductName = ProductName("cheerios")
  private val cheeriosPrice = Money(8.43)
  private val quantityOne = Quantity(1)
  private val taxRate = TaxRate("12.5")
  private val subTotalOneCheerios = cheeriosPrice * quantityOne.value
  private val taxAmountOneCheerios = cheeriosPrice * taxRate
  private val taxOneCheerios = Tax(taxRate, taxAmountOneCheerios)
  private val cheeriosProduct = Product(cheeriosProductName, cheeriosPrice)
  private val itemOneCheerios = Item(cheeriosProduct, quantityOne)
  private val cartTotalsOneCheerios = CartTotals(subTotal, tax, total = subTotal + tax)
  private def cartOneCheerios(cartId: UUID) = {
    Cart(
      id = cartId,
      items = Map(cheeriosProductName -> itemOneCheerios),
      totals = cartTotalsOneCheerios
    )
  }

  test("New empty cart can be created") {
    for {
      cart <- cartService.create()
    } yield assert(cart === Cart.empty)
  }

  test("Product can be added to the cart by name and quantity") {
    for {
      cart <- cartService.create()
      cart <- cartService.add(cart.id, cheeriosPrice, quantityOne)
    } yield assert(cart.items(cheeriosProductName).quantity === quantityOne)
  }

  test("Product price should be found by product name") {

    val quantity = Quantity(1)
    for {
      cart <- cartService.create()
      updatedCart <- cartService.add(cart.id, cheeriosProductName, quantityOne)
    } yield assert(updatedCart.items(cheeriosProductName).product.price === cheeriosPrice)
  }

  test("Cart state must be available upon adding a product") {

    for {
      cart <- cartService.create()
      updatedCart <- cartService.add(cart.id, cheeriosProductName, quantityOne)
    } yield assert(updatedCart === cartOneCheerios(cart.id))
  }

  test("Cart subtotal should be the sum of price for all items") {
    for {
      cart <- cartService.create()
      updatedCart <- cartService.add(cart.id, cheeriosProductName, quantityOne)
    } yield assert(updatedCart.totals.subTotal === subTotalOneCheerios)
  }

  test("Tax should be calculated on subtotal") {
    for {
      cart <- cartService.create()
      updatedCart <- cartService.add(cart.id, cheeriosProductName, quantityOne)
    } yield assert(updatedCart.totals.tax.amount === taxAmountOneCheerios)
  }

  test("Total should be tax amount + subtotal") {
    for {
      cart <- cartService.create()
      updatedCart <- cartService.add(cart.id, cheeriosProductName, quantityOne)
    } yield assert(updatedCart.totals.total === subTotalOneCheerios + taxAmountOneCheerios)
  }

  test("Adding product to non existing cart should fail") {
    val nonExistingCartId = "non-existing-cart-id"
    interceptMessageIO[NoCartError](s"Cart with id: $nonExistingCartId doesn't exist.") {
      for {
        _ <- cartService.add(nonExistingCartId, cheeriosProductName, quantityOne)
      } yield ()
    }
  }

  test("Adding non existing product should fail") {
    val nonExistingProductName = ProductName("non-existing-product-name")
    interceptMessageIO[NoProductError](s"Product with name: $nonExistingProductName doesn't exist.") {
      for {
        cart <- cartService.create()
        _ <- cartService.add(cart.id, cheeriosProductName, quantityOne)
      } yield ()
    }
  }


}
