package com.csse3200.game.items.consumables;

import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.items.ItemIds;

/** Temporarily increases movement speed. */
public final class SpeedPotion extends StatBoostPotion {
  private static final long DEFAULT_DURATION_MS = 8000;

  public SpeedPotion(int quantity) {
    this(quantity, DEFAULT_DURATION_MS);
  }

  public SpeedPotion(int quantity, long durationMs) {
    super(
        ItemIds.SPEED_POTION,
        "Speed Potion",
        "Temporarily increases movement speed.",
        "images/speed_potion_pixel.png",
        quantity,
        Stat.MOVEMENT_SPEED,
        1.5f,
        durationMs);
  }
}
