package com.csse3200.game.items;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import java.util.Objects;

/** Stackable consumable in the world; using it is handled by the player's effect component. */
public final class ConsumableItem extends Item {
  private final ItemType itemType;
  private final int quantity;

  public ConsumableItem(ItemType itemType, int quantity) {
    super(
        Objects.requireNonNull(itemType, "itemType cannot be null").getDisplayName(),
        itemType.getDescription(),
        itemType.getTexturePath());
    if (!itemType.isConsumable() || quantity <= 0) {
      throw new IllegalArgumentException(
          "ConsumableItem requires a consumable ID and positive quantity");
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
    player.getComponent(InventoryComponent.class).addConsumable(itemType, quantity);
  }

  @Override
  public void drop(Entity player) {
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    for (int i = 0; i < quantity; i++) {
      inventory.removeConsumable(itemType);
    }
  }
}
