package com.csse3200.game.items.consumables;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.services.GameTime;

/** Shared timed multiplier behaviour for potions that boost one stat. */
abstract class StatBoostPotion extends ConsumableItem {
  private final Stat boostedStat;
  private final float multiplier;
  private final long durationMs;

  StatBoostPotion(
      ItemType type, int quantity, Stat boostedStat, float multiplier, long durationMs) {
    super(type, quantity);
    this.boostedStat = boostedStat;
    this.multiplier = multiplier;
    this.durationMs = durationMs;
  }

  @Override
  public TimedStatusEffect use(CombatStatsComponent stats, GameTime time) {
    return new StatBoostEffect(time, durationMs, boostedStat, multiplier);
  }

  private static final class StatBoostEffect extends TimedStatusEffect {
    private final Stat boostedStat;
    private final float multiplier;

    StatBoostEffect(GameTime time, long durationMs, Stat boostedStat, float multiplier) {
      super(time, durationMs);
      this.boostedStat = boostedStat;
      this.multiplier = multiplier;
    }

    @Override
    public float getStatMultiplier(Stat stat) {
      return stat == boostedStat ? multiplier : 1f;
    }
  }
}
