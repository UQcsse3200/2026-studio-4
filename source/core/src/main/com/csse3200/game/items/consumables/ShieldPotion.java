package com.csse3200.game.items.consumables;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.statuseffects.*;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.services.GameTime;

/** Blocks incoming damage for a fixed duration. */
public final class ShieldPotion extends ConsumableItem {
  private static final long DEFAULT_DURATION_MS = 8000;
  private final long durationMs;

  public ShieldPotion(int quantity) {
    this(quantity, DEFAULT_DURATION_MS);
  }

  public ShieldPotion(int quantity, long durationMs) {
    super(
        ItemIds.SHIELD,
        "Shield",
        "Provides temporary protection when consumed.",
        "images/items/consumables/shield_consumable_pixel.png",
        quantity);
    this.durationMs = durationMs;
  }

  @Override
  public StatusEffect use(CombatStatsComponent stats, GameTime time) {
    return new Shield(time, durationMs);
  }
}
