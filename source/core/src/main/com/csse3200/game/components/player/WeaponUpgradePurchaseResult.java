package com.csse3200.game.components.player;

/** Outcome of paying inventory gold for an existing weapon upgrade. */
public enum WeaponUpgradePurchaseResult {
  SUCCESS,
  INSUFFICIENT_GOLD,
  ALREADY_UPGRADED,
  INVALID_WEAPON,
  INVALID_PRICE
}
