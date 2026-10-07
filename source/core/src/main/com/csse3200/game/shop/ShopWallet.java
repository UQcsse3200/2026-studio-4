package com.csse3200.game.shop;

/**
 * The wallet/inventory operations the shop purchase transaction needs. Implemented by a small
 * adapter (InventoryShopWallet) around the real InventoryComponent, so ShopService itself never
 * depends on inventory internals and can be tested in isolation.
 */
public interface ShopWallet {
  /** True if the wallet currently holds at least {@code amount} gold. */
  boolean hasGold(int amount);

  /** Deducts {@code amount} gold. Caller must have already checked {@link #hasGold}. */
  void spendGold(int amount);

  /** Grants {@code quantity} of the item identified by {@code itemId}. */
  void grantItem(String itemId, int quantity);
}
