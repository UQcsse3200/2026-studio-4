package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.ItemType;

/** Three reassignable quick-use slots. Inventory quantities remain owned by InventoryComponent. */
public class ConsumableLoadoutComponent extends Component {
  public static final String CHANGED = "consumableLoadoutChanged";
  private final ItemType[] slots = {ItemType.HEALTH_POTION, ItemType.SHIELD, ItemType.SPEED_POTION};

  /** Returns a slot's assignment; a null assignment denotes an empty slot. */
  public ItemType getSlot(int slot) {
    checkSlot(slot);
    return slots[slot];
  }

  /** Assigns a consumable (even if out of stock), or clears a slot with null. */
  public void assignSlot(int slot, ItemType type) {
    checkSlot(slot);
    if (type != null && !type.isConsumable()) {
      throw new IllegalArgumentException("Only consumables can be assigned");
    }
    slots[slot] = type;
    if (entity != null) {
      entity.getEvents().trigger(CHANGED);
    }
  }

  /** Requests use; only the effect component can apply and debit a consumable. */
  public boolean useSlot(int slot) {
    checkSlot(slot);
    ConsumableEffectComponent effects = entity.getComponent(ConsumableEffectComponent.class);
    return effects != null && effects.tryUse(slots[slot]);
  }

  /** Cycles the assigned type without consuming it, for the quick-bar assignment control. */
  public void cycleSlot(int slot) {
    ItemType current = getSlot(slot);
    ItemType[] types = {
      ItemType.HEALTH_POTION, ItemType.SHIELD, ItemType.SPEED_POTION, ItemType.STRENGTH_POTION
    };
    for (int i = 0; i < types.length; i++) {
      if (types[i] == current) {
        assignSlot(slot, types[(i + 1) % types.length]);
        return;
      }
    }
    assignSlot(slot, types[0]);
  }

  private static void checkSlot(int slot) {
    if (slot < 0 || slot >= 3) {
      throw new IllegalArgumentException("Slot must be between 0 and 2");
    }
  }
}
