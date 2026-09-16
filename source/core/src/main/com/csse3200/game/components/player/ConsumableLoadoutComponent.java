package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.items.ItemType;

/** Fixed quick-use slots. Only the effect component owns consumption and effect application. */
public class ConsumableLoadoutComponent extends Component {
  private static final ItemType[] SLOTS = {
    ItemType.HEALTH_POTION, ItemType.SHIELD, ItemType.SPEED_POTION, ItemType.STRENGTH_POTION
  };

  /** Returns the fixed consumable for a quick-use slot. */
  public ItemType getSlot(int slot) {
    if (slot < 0 || slot >= SLOTS.length) {
      throw new IllegalArgumentException("Slot must be between 0 and 3");
    }
    return SLOTS[slot];
  }

  /** Requests use without removing stock in the input layer. */
  public boolean useSlot(int slot) {
    ItemType type = getSlot(slot);
    ConsumableEffectComponent effects = entity.getComponent(ConsumableEffectComponent.class);
    return effects != null && effects.tryUse(type);
  }
}
