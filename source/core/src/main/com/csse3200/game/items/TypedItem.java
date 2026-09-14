package com.csse3200.game.items;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import java.util.Objects;

/** A consumable or currency item backed by Team 5's stable {@link ItemType} contract. */
public final class TypedItem extends Item {
  private final ItemType itemType;
  private final int quantity;

  /** Creates an item from a validated drop specification. */
  public TypedItem(ItemDropSpec dropSpec) {
    super(
        Objects.requireNonNull(dropSpec, "dropSpec cannot be null").itemType().getDisplayName(),
        dropSpec.itemType().getDescription(),
        dropSpec.itemType().getTexturePath());
    if (dropSpec.itemType() == ItemType.STRENGTH_CHARM) {
      throw new IllegalArgumentException("Strength Charm uses its dedicated item implementation");
    }
    this.itemType = dropSpec.itemType();
    this.quantity = dropSpec.quantity();
  }

  public ItemType getItemType() {
    return itemType;
  }

  public int getQuantity() {
    return quantity;
  }

  @Override
  public void pickUp(Entity player) {
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    if (itemType.isConsumable()) {
      inventory.addConsumable(itemType, quantity);
    } else if (itemType.isCurrency()) {
      inventory.addGold(quantity);
    }
  }

  @Override
  public void drop(Entity player) {
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    if (itemType.isConsumable()) {
      for (int i = 0; i < quantity; i++) {
        inventory.removeConsumable(itemType);
      }
    } else if (itemType.isCurrency()) {
      inventory.addGold(-quantity);
    }
  }
}
