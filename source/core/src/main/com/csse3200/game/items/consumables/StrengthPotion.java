package com.csse3200.game.items.consumables;

import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.items.ItemIds;

/** Temporarily increases attack strength. */
public final class StrengthPotion extends StatBoostPotion {
  private static final long DEFAULT_DURATION_MS = 8000;

  public StrengthPotion(int quantity) {
    this(quantity, DEFAULT_DURATION_MS);
  }

  public StrengthPotion(int quantity, long durationMs) {
    super(
        ItemIds.STRENGTH_POTION,
        "Strength Potion",
        "Temporarily increases attack strength.",
        "images/strength_potion_pixel.png",
        quantity,
        Stat.ATTACK,
        1.5f,
        durationMs);
  }
}
