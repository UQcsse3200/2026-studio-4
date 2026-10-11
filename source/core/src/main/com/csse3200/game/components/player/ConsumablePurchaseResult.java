package com.csse3200.game.components.player;

/** Outcome of a single-unit gold purchase; rejected transactions leave inventory unchanged. */
public enum ConsumablePurchaseResult {
  SUCCESS,
  INVALID_ITEM,
  INVALID_PRICE,
  INSUFFICIENT_GOLD,
  QUANTITY_LIMIT
}
