package com.csse3200.game.components.shop;

/** Result returned to shop presentation without exposing inventory implementation details. */
public enum ShopPurchaseResult {
  SUCCESS,
  INSUFFICIENT_GOLD,
  ALREADY_UPGRADED,
  INVALID_OFFER,
  UNSUPPORTED_PRODUCT,
  QUANTITY_LIMIT
}
