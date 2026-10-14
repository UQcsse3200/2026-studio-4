package com.csse3200.game.components.player;

/** Outcome of buying one charm; rejected purchases leave gold and ownership unchanged. */
public enum CharmPurchaseResult {
  SUCCESS,
  INVALID_ITEM,
  INVALID_PRICE,
  INSUFFICIENT_GOLD
}
