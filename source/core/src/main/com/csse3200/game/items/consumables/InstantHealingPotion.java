package com.csse3200.game.items.consumables;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.services.GameTime;

/** Restores a fixed amount of health immediately. */
public final class InstantHealingPotion extends ConsumableItem {
  private final int healing;

  public InstantHealingPotion(ItemType type, int quantity, int healing) {
    super(type, quantity);
    if (healing <= 0) {
      throw new IllegalArgumentException("healing must be positive");
    }
    this.healing = healing;
  }

  @Override
  public boolean canUse(
      CombatStatsComponent stats, StatusEffectsControllerComponent effects, GameTime time) {
    return stats.getHealth() < stats.getMaxHealth();
  }

  @Override
  public TimedStatusEffect use(CombatStatsComponent stats, GameTime time) {
    stats.addHealth(healing);
    return null;
  }
}
