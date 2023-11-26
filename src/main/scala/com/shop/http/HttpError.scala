package com.shop.http

import com.shop.ShopCartError

abstract class HttpError(override val message: String, cause: Option[Exception] = None) extends ShopCartError(message, cause)
abstract class HttpClientError(override val message: String, cause: Option[Exception] = None) extends HttpError(message, cause)
