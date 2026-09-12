package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;

/**
 * Passive ability. Once unlocked, a hostile hit that leaves the player on a sliver of health starts
 * a short burst that amplifies effective strength, movement speed and attack speed. It reads raw
 * combat stats rather than changing them, so the burst cannot leak into the player's real stats.
 */
public final class LastStand extends PlayerAbility {
  /** Name carried by the abilityUsed and abilityEnded events. */
  public static final String NAME = "laststand";

  public static final long DURATION_MS = 10_000;
  public static final long COOLDOWN_MS = 60_000;

  /** Applied to effective strength, movement speed and attack speed while the burst runs. */
  public static final float MULTIPLIER = 1.5f;

  /** Share of max health at or below which a hostile hit starts the burst. */
  private static final int HEALTH_PERCENT = 20;

  public LastStand(GameTime time) {
    super(NAME, time, DURATION_MS, COOLDOWN_MS, false);
  }

  /** Starts only on a hostile hit that leaves the player alive and under the health threshold. */
  @Override
  public boolean triggersOnDamage(
      CombatStatsComponent stats, Entity attacker, int healthLost, int remainingHealth) {
    return healthLost > 0
        && remainingHealth > 0
        && (long) remainingHealth * 100 < (long) stats.getMaxHealth() * HEALTH_PERCENT
        && CombatStatsComponent.isHostileAttacker(attacker);
  }

  /** Returns whether the amplifier is running on an entity. */
  public static boolean isActiveOn(Entity target) {
    return isRunningOn(target, LastStand.class);
  }
}
