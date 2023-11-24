package com.shop
package service

import cats.effect.IO
import com.shop.TestData._
import com.shop.config.Config.taxRate
import com.shop.generators._
import com.shop.model.cart._
import com.shop.model.error.{CartError, ProductError}
import com.shop.model.moneyContext
import com.shop.model.product.ProductName
import com.shop.model.tax.Tax
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.effect.PropF.forAllF
import squants.market.Money

import java.util.UUID

class CartServiceTest extends CatsEffectSuite with ScalaCheckEffectSuite {

  private def cartService(cart: Cart) = new CartService[IO] {
    override def createCart(): IO[Cart] = IO.pure(Cart.empty(cart.id, taxRate))

    override def addProduct(cartId: CartId, productName: ProductName, quantity: Quantity): IO[Cart] = IO.pure(cart)

    override def getCart(cartId: CartId): IO[Cart] = IO.pure(cart)
  }
  // createCart
  test("New empty cart can be created") {
    forAllF { (cart: Cart) =>
      for {
        obtainedCart <- cartService(cart).createCart()
      } yield {
        val expectedCart = Cart(obtainedCart.id, Map.empty, CartTotals(MoneyOps.zero, Tax(taxRate, MoneyOps.zero), MoneyOps.zero))
        assertEquals(obtainedCart, expectedCart)
      }
    }
  }

  // addProduct
  test("Product can be added to an empty cart by name and quantity") {
    forAllF { (cart: Cart, quantity: Quantity) =>
      for {
        emptyCart <- cartService(cart).createCart()
        updatedCart <- cartService(emptyCart).addProduct(emptyCart.id, cheerios, quantity)
      } yield assertEquals(updatedCart.items(cheerios).quantity, quantity)
    }
  }

  test("Product can be added to a non empty cart by name and quantity") {
    forAllF { (cart: Cart, quantity: Quantity) =>
      val item = cart.items.head._2
      val productName = item.product.name
      val originalQuantity = item.quantity
      val expectedQuantity = Quantity(Refined.unsafeApply(originalQuantity.value + quantity.value))
      val nonEmptyCart = cartService(cart)
      for {
        result <- nonEmptyCart.addProduct(cart.id, productName, quantity)
      } yield {
        val obtainedQuantity = result.items(productName).quantity
        assertEquals(obtainedQuantity, expectedQuantity)
      }
    }
  }

  test("Product price should be found by product name") {
    forAllF { (cart: Cart, quantity: Quantity) =>
      for {
        emptyCart <- cartService(cart).createCart()
        updatedCart <- cartService(emptyCart).addProduct(emptyCart.id, cheerios, quantity)
      } yield assertEquals(updatedCart.items(cheerios).product.price, productPrices(cheerios))
    }
  }

  test("Cart state must be available upon adding a product") {
    forAllF { (cart: Cart, quantity: Quantity) =>
      for {
        emptyCart <- cartService(cart).createCart()
        updatedCart <- cartService(emptyCart).addProduct(emptyCart.id, cheerios, quantity)
      } yield assertEquals(updatedCart, cart)
    }
  }

  test("Cart subtotal should be the sum of price for all items") {
    forAllF { (cart: Cart, quantity: Quantity) =>
      for {
        emptyCart <- cartService(cart).createCart()
        _ <- cartService(emptyCart).addProduct(emptyCart.id, cheerios, quantity)
        updatedCart <- cartService(emptyCart).addProduct(emptyCart.id, weetabix, quantity)
      } yield assertEquals(updatedCart.totals.subTotal, productPrices(cheerios) + productPrices(weetabix))
    }
  }

  test("Tax should be calculated on subtotal") {
    forAllF { (cart: Cart, quantity: Quantity) =>
      for {
        emptyCart <- cartService(cart).createCart()
        updatedCart <- cartService(emptyCart).addProduct(emptyCart.id, weetabix, quantity)
      } yield assertEquals(updatedCart.totals.tax.amount, productPrices(weetabix) * taxRate.value)
    }
  }

  test("Total should be tax amount + subtotal") {
    forAllF { (cart: Cart, quantity: Quantity) =>
      for {
        emptyCart <- cartService(cart).createCart()
        updatedCart <- cartService(emptyCart).addProduct(emptyCart.id, weetabix, quantity)
      } yield assertEquals(updatedCart.totals.subTotal, productPrices(weetabix) + productPrices(weetabix) * taxRate.value)
    }
  }

  test("Adding product to non existing cart should fail") {
    forAllF { (cart: Cart, quantity: Quantity) =>
      val nonExistingCartId = CartId(UUID.nameUUIDFromBytes("non-existing-cart-id".getBytes))
      val expectedError = CartError(nonExistingCartId, "")
      cartService(cart).addProduct(nonExistingCartId, weetabix, quantity)
        .attempt
        .map {
          case Left(e) => assertEquals(e, expectedError)
          case Right(_) => fail("expected test failure")
        }
    }
  }

  test("Adding non existing product should fail") {
    forAllF { (cart: Cart, cartId: CartId, quantity: Quantity) =>
      val nonExistingProduct = ProductName("non-existing-product-name")
      val expectedError = ProductError(nonExistingProduct, "")
      cartService(cart).addProduct(cartId, nonExistingProduct, quantity)
        .attempt
        .map {
          case Left(e) => assertEquals(e, expectedError)
          case Right(_) => fail("expected test failure")
        }
    }
  }

  // getCart
  test("Calculate totals") {
    forAllF { (cart: Cart, cartId: CartId, quantity: Quantity) =>
      val quantityOne = Quantity(1)
      for {
        cart <- cartService(cart).createCart()
        _ <- cartService(cart).addProduct(cart.id, ProductName("cornflakes"), quantityOne)
        _ <- cartService(cart).addProduct(cart.id, ProductName("cornflakes"), quantityOne)
        _ <- cartService(cart).addProduct(cart.id, ProductName("weetabix"), quantityOne)
        updatedCart <- cartService(cart).getCart(cart.id)
      } yield assertEquals(updatedCart.totals, CartTotals(subTotal = Money(15.02), tax = Tax(taxRate, Money(1.88)), total = Money(16.90)))
    }
  }

}
