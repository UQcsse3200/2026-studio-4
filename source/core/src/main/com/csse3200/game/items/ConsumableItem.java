package com.csse3200.game.items;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;
import java.util.Objects;

/** Shared pickup and inventory behaviour for consumables with subtype-specific use effects. */
public abstract class ConsumableItem extends Item {
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

  /** Whether this item can be consumed with the player's current state. */
  public boolean canUse(
      CombatStatsComponent stats, StatusEffectsControllerComponent effects, GameTime time) {
    return effects != null && !effects.isDisposed() && time != null;
  }

  /** Applies the item's effect; timed effects are registered by the use component. */
  public abstract TimedStatusEffect use(CombatStatsComponent stats, GameTime time);

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
