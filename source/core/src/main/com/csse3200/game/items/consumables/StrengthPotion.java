package com.csse3200.game.items.consumables;

import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.items.ItemType;

/** Temporarily increases attack strength. */
public final class StrengthPotion extends StatBoostPotion {
  public StrengthPotion(ItemType type, int quantity, long durationMs) {
    super(type, quantity, Stat.ATTACK, 1.5f, durationMs);
  }
}
