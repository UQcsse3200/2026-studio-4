package com.csse3200.game.shop;

/**
 * The wallet operations the gamble transaction needs. Deliberately the same shape as ShopWallet so
 * both can share a real adapter around InventoryComponent.
 */
public interface GambleWallet {
  boolean hasGold(int amount);

  void spendGold(int amount);

  void grantItem(String itemId, int quantity);
}
