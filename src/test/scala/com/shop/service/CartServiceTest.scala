package com.shop
package service

import cats.effect.IO
import cats.implicits._
import com.shop.TestData._
import com.shop.config.Config.{CartConfig, taxRate}
import com.shop.generators._
import com.shop.http.PricesClient
import com.shop.http.error.PricesClientError
import com.shop.model.cart._
import com.shop.model.product.{ProductName, ShoppingProduct}
import com.shop.model.tax.Tax
import com.shop.model.{moneyConfig, moneyContext}
import com.shop.repo.CartRepo
import com.shop.repo.error.CartNotFound
import eu.timepit.refined.auto._
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.effect.PropF.forAllF
import squants.market.Money

import java.util.UUID

class CartServiceTest extends CatsEffectSuite with ScalaCheckEffectSuite {

  // createCart
  test("New empty cart can be created") {
    for {
      obtainedCart <- cartService().createCart()
    } yield {
      assertEquals(obtainedCart, anEmptyCart(obtainedCart.id))
    }
  }

  // addProduct
  test("Product can be added to an empty cart by name and quantity") {
    forAllF { (productName: ProductName, quantity: Quantity) =>
      for {
        emptyCart <- cartService().createCart()
        updatedCart <- cartService(aCartRepo(emptyCart)).addProduct(emptyCart.id, productName, quantity)
      } yield assertEquals(updatedCart.items(productName).quantity, quantity)
    }
  }

  test("Product can be added to a non empty cart by name and quantity") {
    forAllF { (cart: Cart, quantity: Quantity) =>
      val item = cart.items.head._2
      val productName = item.product.name
      val originalQuantity = item.quantity
      val expectedQuantity = originalQuantity |+| quantity
      val nonEmptyCartService = cartService(aCartRepo(cart))
      for {
        result <- nonEmptyCartService.addProduct(cart.id, productName, quantity)
      } yield {
        val obtainedQuantity = result.items(productName).quantity
        assertEquals(obtainedQuantity, expectedQuantity)
      }
    }
  }

  test("Product price should be found by product name") {
    forAllF { (productName: ProductName, quantity: Quantity) =>
      for {
        emptyCart <- cartService().createCart()
        updatedCart <- cartService(aCartRepo(emptyCart)).addProduct(emptyCart.id, productName, quantity)
      } yield assertEquals(updatedCart.items(productName).product.price, productPrices(productName))
    }
  }

  test("Cart state (items & totals) must be available when adding a product") {
    forAllF { (productName: ProductName, quantity: Quantity) =>
      def expectedCart(id: CartId) = {
        val productPrice = productPrices(productName)
        val subtotal = productPrice * quantity.value.value
        val taxAmount = subtotal * taxRate.value
        val total = subtotal + taxAmount
        Cart(id, Map(productName -> Item(ShoppingProduct(productName, productPrice), quantity)), CartTotals(subtotal, Tax(taxRate, taxAmount), total))
      }

      for {
        emptyCart <- cartService().createCart()
        obtainedCart <- cartService(aCartRepo(emptyCart)).addProduct(emptyCart.id, productName, quantity)
      } yield assertEquals(obtainedCart, expectedCart(obtainedCart.id))
    }
  }

  test("Cart subtotal should be the sum of price for all items") {
    forAllF { (productName: ProductName, quantity: Quantity) =>
      val expectedSubtotal = productPrices(productName) * quantity.value.value
      for {
        emptyCart <- cartService().createCart()
        obtainedCart <- cartService(aCartRepo(emptyCart)).addProduct(emptyCart.id, productName, quantity)
      } yield {
        assertEquals(obtainedCart.totals.subTotal, expectedSubtotal)
      }
    }
  }

  test("Tax should be calculated on subtotal") {
    forAllF { (productName: ProductName, quantity: Quantity) =>
      val subtotal = productPrices(productName) * quantity.value.value
      val expectedTaxAmount = subtotal * taxRate.value
      for {
        emptyCart <- cartService().createCart()
        obtainedCart <- cartService(aCartRepo(emptyCart)).addProduct(emptyCart.id, productName, quantity)
      } yield assertEquals(obtainedCart.totals.tax.amount, expectedTaxAmount)
    }
  }

  test("Total should be tax amount + subtotal") {
    forAllF { (productName: ProductName, quantity: Quantity) =>
      val subtotal = productPrices(productName) * quantity.value.value
      val taxAmount = subtotal * taxRate.value
      val expectedTotal = subtotal + taxAmount
      for {
        emptyCart <- cartService().createCart()
        updatedCart <- cartService(aCartRepo(emptyCart)).addProduct(emptyCart.id, productName, quantity)
      } yield assertEquals(updatedCart.totals.total, expectedTotal)
    }
  }

  test("Adding product to non existing cart should fail") {
    val cartId = aCartId
    val cartNotFoundRepo = new TestCartRepo() {
      override def readCart(cartId: CartId): IO[Cart] = CartNotFound(cartId).raiseError[IO, Cart]
    }
    interceptMessageIO[CartNotFound](s"Cart with id: $cartId not found.") {
      cartService(cartNotFoundRepo).addProduct(cartId, weetabix, Quantity(1))
    }
  }

  test("Adding a product with no price should fail") {
    val productName = ProductName("no-price-product")
    val error = PricesClientError(productName, "errorMsg")

    val priceNotFoundClient = new TestPricesClient {
      override def getPrice(productName: ProductName): IO[Money] = error.raiseError[IO, Money]
    }
    interceptMessageIO[PricesClientError](error.message) {
      cartService(pricesClient = priceNotFoundClient).addProduct(aCartId, weetabix, Quantity(1))
    }
  }

  // getCart
  test("Calculated totals should be rounded") {
    val cartId = aCartId
    val quantityOne = Quantity(1)
    for {
      emtpyCart <- cartService().createCart()
      c1 <- cartService(aCartRepo(emtpyCart)).addProduct(cartId, cornflakes, quantityOne)
      c2 <- cartService(aCartRepo(c1)).addProduct(cartId, cornflakes, quantityOne)
      c3 <- cartService(aCartRepo(c2)).addProduct(cartId, weetabix, quantityOne)
      updatedCart <- cartService(aCartRepo(c3)).getCart(cartId)
    } yield assertEquals(updatedCart.totals.rounded, CartTotals(subTotal = Money(15.02), tax = Tax(taxRate, Money(1.88)), total = Money(16.90)))
  }

  test("Getting a non existing cart should fail") {
    val cartId = aCartId
    val cartNotFoundRepo = new TestCartRepo() {
      override def readCart(cartId: CartId): IO[Cart] = CartNotFound(cartId).raiseError[IO, Cart]
    }
    interceptMessageIO[CartNotFound](s"Cart with id: $cartId not found.") {
      cartService(cartNotFoundRepo).getCart(cartId)
    }
  }

  private def cartService(cartRepo: CartRepo[IO] = aCartRepo(), pricesClient: PricesClient[IO] = new TestPricesClient): CartService[IO] = CartService.make[IO](pricesClient, cartRepo, CartConfig())

  private def pricesClientError(productName: ProductName): PricesClientError = PricesClientError(productName, "errorMsg")

  protected class TestPricesClient extends PricesClient[IO] {
    override def getPrice(productName: ProductName): IO[Money] = IO.pure(productPrices(productName))
  }

  def aCartId: CartId = CartId(UUID.randomUUID)

  private def anEmptyCart(id: CartId = aCartId): Cart = Cart.empty(id, taxRate)

  /* TODO: test this scenario on the repo
  private val cartRepoRepoStubErrorCartModified = new TestCartRepo() {
    override def replaceCart(oldCart: Cart, newCart: Cart): IO[Unit] = CartModified(oldCart, newCart).raiseError[IO, Unit]
  }*/

  private def aCartRepo(cart: Cart = anEmptyCart()) = new TestCartRepo(cart)

  protected class TestCartRepo(cart: Cart = anEmptyCart()) extends CartRepo[IO] {
    override def createCart(): IO[Cart] = IO.pure(anEmptyCart())

    override def readCart(cartId: CartId): IO[Cart] = IO.pure(cart)

    override def replaceCart(oldCart: Cart, newCart: Cart): IO[Unit] = IO.unit
  }

}
