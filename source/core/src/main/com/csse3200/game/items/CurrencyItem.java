package com.csse3200.game.items;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import java.util.Objects;

/** Stackable currency represented by the same world-item and pickup contract. */
public final class CurrencyItem extends Item {
  private final ItemType itemType;
  private final int quantity;

  public CurrencyItem(ItemType itemType, int quantity) {
    super(
        Objects.requireNonNull(itemType, "itemType cannot be null").getDisplayName(),
        itemType.getDescription(),
        itemType.getTexturePath());
    if (!itemType.isCurrency() || quantity <= 0) {
      throw new IllegalArgumentException(
          "CurrencyItem requires a currency ID and positive quantity");
    }
    this.itemType = itemType;
    this.quantity = quantity;
  }

  @Override
  public ItemType getItemType() {
    return itemType;
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
