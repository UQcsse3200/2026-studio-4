package com.csse3200.game.shop;

/**
 * Inventory boundary for Dev's item gambling rules; settlement must commit before notifications.
 */
public interface GambleWallet {
  boolean hasGold(int amount);

  /** Resolves debit and any reward together; a rejection must leave the inventory unchanged. */
  GambleResult.Status settle(GambleEntry entry, int cost);
}
