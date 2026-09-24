package com.csse3200.game.items.consumables;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.services.GameTime;

/** Restores health in regular ticks during a timed effect. */
public abstract class RegenerationPotion extends ConsumableItem {
  private static final long TICK_MS = 1000;
  private final int healingPerTick;
  private final long durationMs;

  protected RegenerationPotion(ItemType type, int quantity, int healingPerTick, long durationMs) {
    super(type, quantity);
    if (healingPerTick <= 0 || durationMs < TICK_MS) {
      throw new IllegalArgumentException(
          "regeneration needs positive healing and at least one tick");
    }
    this.healingPerTick = healingPerTick;
    this.durationMs = durationMs;
  }

  @Override
  public boolean canUse(
      CombatStatsComponent stats, StatusEffectsControllerComponent effects, GameTime time) {
    return stats.getHealth() < stats.getMaxHealth() && super.canUse(stats, effects, time);
  }

  @Override
  public TimedStatusEffect use(CombatStatsComponent stats, GameTime time) {
    return new HealingOverTime(time, durationMs, stats, healingPerTick);
  }

  private static final class HealingOverTime extends TimedStatusEffect {
    private final GameTime time;
    private final CombatStatsComponent stats;
    private final int healingPerTick;
    private final long endsAt;
    private long nextTick;

    HealingOverTime(
        GameTime time, long durationMs, CombatStatsComponent stats, int healingPerTick) {
      super(time, durationMs);
      this.time = time;
      this.stats = stats;
      this.healingPerTick = healingPerTick;
      endsAt = time.getTime() + durationMs;
      nextTick = time.getTime() + TICK_MS;
    }

    @Override
    public boolean update() {
      long through = Math.min(time.getTime(), endsAt);
      while (nextTick <= through && !stats.isDead()) {
        stats.addHealth(healingPerTick);
        nextTick += TICK_MS;
      }
      return super.update();
    }
  }
}
