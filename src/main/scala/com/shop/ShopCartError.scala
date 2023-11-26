package com.shop

abstract class ShopCartError(val message: String, val cause: Option[Exception] = None) extends Exception(message, cause.orNull)
