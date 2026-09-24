package com.csse3200.game.items.consumables;

import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.items.ItemType;

/** Temporarily increases movement speed. */
public final class SpeedPotion extends StatBoostPotion {
  public SpeedPotion(ItemType type, int quantity, long durationMs) {
    super(type, quantity, Stat.MOVEMENT_SPEED, 1.5f, durationMs);
  }
}
