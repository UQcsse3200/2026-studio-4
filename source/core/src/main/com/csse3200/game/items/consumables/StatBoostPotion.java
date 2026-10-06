package com.csse3200.game.items.consumables;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.services.GameTime;

/** Shared timed multiplier behaviour for potions that boost one stat. */
abstract class StatBoostPotion extends ConsumableItem {
  /** The stat, magnitude and lifetime of one timed boost. */
  record Boost(Stat stat, float multiplier, long durationMs) {}

  private final Boost boost;

  StatBoostPotion(
      String id, String name, String description, String texture, int quantity, Boost boost) {
    super(id, name, description, texture, quantity);
    this.boost = boost;
  }

  @Override
  public TimedStatusEffect use(CombatStatsComponent stats, GameTime time) {
    return new StatBoostEffect(time, boost);
  }

  private static final class StatBoostEffect extends TimedStatusEffect {
    private final Boost boost;

    StatBoostEffect(GameTime time, Boost boost) {
      super(time, boost.durationMs());
      this.boost = boost;
    }

    @Override
    public float getStatMultiplier(Stat stat) {
      return stat == boost.stat() ? boost.multiplier() : 1f;
    }
  }
}
