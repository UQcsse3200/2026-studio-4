package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;

/** Adds one independent charm through the inventory's gold transaction. */
public class CharmPurchaseEffect implements ShopPurchaseEffect {
  @Override
  public ShopPurchaseResult purchase(
      InventoryComponent inventory, String productId, int goldPrice) {
    return switch (inventory.tryPurchaseCharm(productId, goldPrice)) {
      case SUCCESS -> ShopPurchaseResult.SUCCESS;
      case INSUFFICIENT_GOLD -> ShopPurchaseResult.INSUFFICIENT_GOLD;
      case INVALID_ITEM, INVALID_PRICE -> ShopPurchaseResult.INVALID_OFFER;
    };
  }
}
