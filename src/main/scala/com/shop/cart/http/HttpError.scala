package com.shop.cart.http

import com.shop.cart.error.CartError

abstract class HttpError(override val message: String, cause: Option[Exception] = None) extends CartError(message, cause)
abstract class HttpClientError(override val message: String, cause: Option[Exception] = None) extends HttpError(message, cause)
