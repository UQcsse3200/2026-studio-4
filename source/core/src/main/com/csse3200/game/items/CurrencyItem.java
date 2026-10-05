package com.csse3200.game.items;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;

/** Stackable currency represented by the same world-item and pickup contract. */
public final class CurrencyItem extends Item {
  private final int quantity;

  public CurrencyItem(int quantity) {
    super(
        ItemIds.GOLD_COIN,
        "Gold Coin",
        "Currency dropped by defeated enemies.",
        "images/gold_coin_pixel.png",
        ItemCategory.CURRENCY);
    if (quantity <= 0) {
      throw new IllegalArgumentException("CurrencyItem requires positive quantity");
    }
    this.quantity = quantity;
  }

  @Override
  public int getQuantity() {
    return quantity;
  }

  @Override
  public void pickUp(Entity player) {
    player.getComponent(InventoryComponent.class).addGold(quantity);
  }

  @Override
  public void drop(Entity player) {
    player.getComponent(InventoryComponent.class).addGold(-quantity);
  }
}
