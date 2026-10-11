package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;

/**
 * Extension point for a product category. Implementations validate before committing gold and
 * fulfillment together; a rejected result must leave the player's state unchanged.
 */
@FunctionalInterface
public interface ShopPurchaseEffect {
  ShopPurchaseResult purchase(InventoryComponent inventory, String productId, int goldPrice);
}
