package com.shop.cart.service

import cats.effect.IO
import cats.implicits._
import com.shop.cart.TestData._
import com.shop.cart.config.Config.{CartConfig, taxRate}
import com.shop.cart.config.Implicits.{moneyConfig, moneyContext}
import com.shop.cart.generators._
import com.shop.cart.http.PricesClient
import com.shop.cart.http.error.PricesClientError
import com.shop.cart.model.cart._
import com.shop.cart.model.product.{ProductName, ShoppingProduct}
import com.shop.cart.model.tax.Tax
import com.shop.cart.repo.CartRepo
import com.shop.cart.repo.error.CartNotFound
import eu.timepit.refined.auto._
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.effect.PropF.forAllF
import squants.market.Money

class CartServiceTest extends CatsEffectSuite with ScalaCheckEffectSuite {

  // createCart
  test("Creates a new empty cart") {
    for {
      obtainedCart <- cartService().createCart()
    } yield {
      assertEquals(obtainedCart, anEmptyCart(obtainedCart.id))
    }
  }

  // addProduct
  test("Adds a product to an empty cart by name and quantity") {
    forAllF { (productName: ProductName, quantity: Quantity) =>
      for {
        emptyCart <- cartService().createCart()
        updatedCart <- cartService(aCartRepo(emptyCart)).addProduct(emptyCart.id, productName, quantity)
      } yield assertEquals(updatedCart.items(productName).quantity, quantity)
    }
  }

  test("Adds a product to a non empty cart by name and quantity") {
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

  test("Finds a product price by product name") {
    forAllF { (productName: ProductName, quantity: Quantity) =>
      for {
        emptyCart <- cartService().createCart()
        updatedCart <- cartService(aCartRepo(emptyCart)).addProduct(emptyCart.id, productName, quantity)
      } yield assertEquals(updatedCart.items(productName).product.price, productPrices(productName))
    }
  }

  test("Returns cart state (items & totals) when adding a product") {
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

  test("Returns cart subtotal as the sum of price for all items") {
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

  test("Returns tax calculated on subtotal") {
    forAllF { (productName: ProductName, quantity: Quantity) =>
      val subtotal = productPrices(productName) * quantity.value.value
      val expectedTaxAmount = subtotal * taxRate.value
      for {
        emptyCart <- cartService().createCart()
        obtainedCart <- cartService(aCartRepo(emptyCart)).addProduct(emptyCart.id, productName, quantity)
      } yield assertEquals(obtainedCart.totals.tax.amount, expectedTaxAmount)
    }
  }

  test("Returns total as tax amount + subtotal") {
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

  test("Returns error on attempt to add a product to a non existing cart") {
    val cartId = aCartId
    val cartNotFoundRepo = new TestCartRepo() {
      override def getCart(cartId: CartId): IO[Cart] = CartNotFound(cartId).raiseError[IO, Cart]
    }
    interceptMessageIO[CartNotFound](s"Cart with id: $cartId not found.") {
      cartService(cartNotFoundRepo).addProduct(cartId, weetabix, Quantity(1))
    }
  }

  test("Returns error on attempt to add a product with price not found") {
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
  test("Calculates multiple products totals rounding half-up") {
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

  test("Returns error on attempt to get a non existing cart") {
    val cartId = aCartId
    val cartNotFoundRepo = new TestCartRepo() {
      override def getCart(cartId: CartId): IO[Cart] = CartNotFound(cartId).raiseError[IO, Cart]
    }
    interceptMessageIO[CartNotFound](s"Cart with id: $cartId not found.") {
      cartService(cartNotFoundRepo).getCart(cartId)
    }
  }

  private def cartService(cartRepo: CartRepo[IO] = aCartRepo(), pricesClient: PricesClient[IO] = new TestPricesClient): CartService[IO] = CartService.make[IO](pricesClient, cartRepo, CartConfig())

  protected class TestPricesClient extends PricesClient[IO] {
    override def getPrice(productName: ProductName): IO[Money] = IO.pure(productPrices(productName))
  }

  /* TODO: test this scenario on the repo
  private val cartRepoRepoStubErrorCartModified = new TestCartRepo() {
    override def replaceCart(oldCart: Cart, newCart: Cart): IO[Unit] = CartModified(oldCart, newCart).raiseError[IO, Unit]
  }*/

  private def aCartRepo(cart: Cart = anEmptyCart()) = new TestCartRepo(cart)

  protected class TestCartRepo(cart: Cart = anEmptyCart()) extends CartRepo[IO] {
    override def createCart(): IO[Cart] = IO.pure(anEmptyCart())

    override def getCart(cartId: CartId): IO[Cart] = IO.pure(cart)

    override def replaceCart(oldCart: Cart, newCart: Cart): IO[Cart] = IO.pure(newCart)
  }

}
