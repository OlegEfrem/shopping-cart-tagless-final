package com.shop.cart.error

abstract class CartError(val message: String, val cause: Option[Exception] = None) extends Exception(message, cause.orNull)
