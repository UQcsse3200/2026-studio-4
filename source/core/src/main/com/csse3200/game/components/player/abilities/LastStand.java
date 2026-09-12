package com.csse3200.game.components.player.abilities;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.TimedPlayerAbility;
import com.csse3200.game.components.statuseffects.LastStandEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;

/**
 * Passive ability. Once unlocked, a hostile hit that leaves the player on a sliver of health puts a
 * LastStandEffect on them, amplifying effective strength, movement speed and attack speed for as
 * long as it runs.
 *
 * <p>The ability owns the effect, decides when a hit should apply it, and reports the player's
 * state from it.
 */
public final class LastStand extends TimedPlayerAbility {
  /** Name carried by the abilityUsed and abilityEnded events. */
  public static final String NAME = "laststand";

  public static final long DURATION_MS = 10_000;
  public static final long COOLDOWN_MS = 60_000;

  /** Applied to effective strength, movement speed and attack speed while the effect runs. */
  public static final float MULTIPLIER = 1.5f;

  /** Share of max health a hostile hit must leave the player strictly below to apply the effect. */
  private static final int HEALTH_PERCENT = 20;

  public LastStand(GameTime time) {
    super(NAME, COOLDOWN_MS, false, new LastStandEffect(time, DURATION_MS));
  }

  /** Applies only on a hostile hit that leaves the player alive and under the health threshold. */
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
