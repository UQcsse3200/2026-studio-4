package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;

/** Fulfills one consumable through the inventory's atomic gold transaction. */
public class ConsumablePurchaseEffect implements ShopPurchaseEffect {
  @Override
  public ShopPurchaseResult purchase(
      InventoryComponent inventory, String productId, int goldPrice) {
    return switch (inventory.tryPurchaseConsumable(productId, goldPrice)) {
      case SUCCESS -> ShopPurchaseResult.SUCCESS;
      case INSUFFICIENT_GOLD -> ShopPurchaseResult.INSUFFICIENT_GOLD;
      case QUANTITY_LIMIT -> ShopPurchaseResult.QUANTITY_LIMIT;
      case INVALID_ITEM, INVALID_PRICE -> ShopPurchaseResult.INVALID_OFFER;
    };
  }
}
