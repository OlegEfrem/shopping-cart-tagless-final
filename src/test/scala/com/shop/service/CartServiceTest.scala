package com.shop.service

import cats.effect.IO
import cats.implicits._
import com.shop.config.Config.taxRate
import com.shop.model._
import com.shop.model.cart._
import com.shop.model.error.{CartError, ProductError}
import com.shop.model.product.{ProductName, ShoppingProduct}
import com.shop.model.tax.Tax
import munit.CatsEffectSuite
import squants.market.{Money}
import java.util.UUID
import eu.timepit.refined.auto._

class CartServiceTest extends CatsEffectSuite {
  private def newCartId(id: String) = CartId(UUID.nameUUIDFromBytes(id.getBytes))
  private val cartId = newCartId("test-cart-id")
  private val cheeriosProductName = ProductName("cheerios")
  private val cheeriosPrice = Money(8.43)
  private val quantityOne = Quantity(1)
  private val subTotalOneCheerios = cheeriosPrice * quantityOne.value.value
  private val taxAmountOneCheerios = cheeriosPrice * taxRate.value
  private val taxOneCheerios = Tax(taxRate, taxAmountOneCheerios)
  private val cheeriosProduct = ShoppingProduct(cheeriosProductName, cheeriosPrice)
  private val itemOneCheerios = Item(cheeriosProduct, quantityOne)
  private val total: Money = subTotalOneCheerios plus taxAmountOneCheerios
  private val cartTotalsOneCheerios = CartTotals(subTotalOneCheerios, taxOneCheerios, total = total)
  private val cartOneCheerios = Cart(cartId, items = Map(cheeriosProductName -> itemOneCheerios), totals = cartTotalsOneCheerios)


  private val cartService = new CartService[IO] {
    override def createCart(): IO[Cart] = IO.pure(Cart.empty(cartId, taxRate))

    override def addProduct(cartId: CartId, productName: ProductName, quantity: Quantity): IO[Cart] = IO.pure(cartOneCheerios)

    override def getCart(cartId: CartId): IO[Cart] = IO.pure(cartOneCheerios)
  }

  test("New empty cart can be created") {
    for {
      cart <- cartService.createCart()
    } yield assert(cart === Cart.empty(cart.id, taxRate))
  }

  test("Product can be added to the cart by name and quantity") {
    for {
      cart <- cartService.createCart()
      cart <- cartService.addProduct(cart.id, cheeriosProductName, quantityOne)
    } yield assert(cart.items(cheeriosProductName).quantity === quantityOne)
  }

  test("Product price should be found by product name") {

    val quantity = Quantity(1)
    for {
      cart <- cartService.createCart()
      updatedCart <- cartService.addProduct(cart.id, cheeriosProductName, quantityOne)
    } yield assert(updatedCart.items(cheeriosProductName).product.price === cheeriosPrice)
  }

  test("Cart state must be available upon adding a product") {

    for {
      cart <- cartService.createCart()
      updatedCart <- cartService.addProduct(cart.id, cheeriosProductName, quantityOne)
    } yield assert(updatedCart === cartOneCheerios)
  }

  test("Cart subtotal should be the sum of price for all items") {
    for {
      cart <- cartService.createCart()
      updatedCart <- cartService.addProduct(cart.id, cheeriosProductName, quantityOne)
    } yield assert(updatedCart.totals.subTotal === subTotalOneCheerios)
  }

  test("Tax should be calculated on subtotal") {
    for {
      cart <- cartService.createCart()
      updatedCart <- cartService.addProduct(cart.id, cheeriosProductName, quantityOne)
    } yield assert(updatedCart.totals.tax.amount === taxAmountOneCheerios)
  }

  test("Total should be tax amount + subtotal") {
    for {
      cart <- cartService.createCart()
      updatedCart <- cartService.addProduct(cart.id, cheeriosProductName, quantityOne)
    } yield assert(updatedCart.totals.total === subTotalOneCheerios + taxAmountOneCheerios)
  }

  test("Calculate totals") {
    for {
      cart <- cartService.createCart()
      _ <- cartService.addProduct(cart.id, ProductName("cornflakes"), quantityOne)
      _ <- cartService.addProduct(cart.id, ProductName("cornflakes"), quantityOne)
      _ <- cartService.addProduct(cart.id, ProductName("weetabix"), quantityOne)
      updatedCart <- cartService.getCart(cart.id)
    } yield assert(updatedCart.totals === CartTotals(subTotal = Money(15.02), tax = Tax(taxRate, Money(1.88)), total = Money(16.90)))
  }

  test("Adding product to non existing cart should fail") {
    val nonExistingCartId = newCartId("non-existing-cart-id")
    interceptMessageIO[CartError](s"Cart with id: $nonExistingCartId doesn't exist.") {
      for {
        _ <- cartService.addProduct(nonExistingCartId, cheeriosProductName, quantityOne)
      } yield ()
    }
  }

  test("Adding non existing product should fail") {
    val nonExistingProductName = ProductName("non-existing-product-name")
    interceptMessageIO[ProductError](s"Product with name: $nonExistingProductName doesn't exist.") {
      for {
        cart <- cartService.createCart()
        _ <- cartService.addProduct(cart.id, cheeriosProductName, quantityOne)
      } yield ()
    }
  }


}
