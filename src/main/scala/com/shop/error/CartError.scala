package com.shop.error

abstract class CartError(val message: String, val cause: Option[Exception] = None) extends Exception(message, cause.orNull)
