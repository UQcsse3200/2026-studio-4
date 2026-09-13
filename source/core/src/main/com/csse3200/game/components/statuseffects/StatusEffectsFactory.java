package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.GameTime;

/** Factory to create the status effects. */
public class StatusEffectsFactory {
  private StatusEffectsFactory() {
    throw new IllegalStateException("Instantiating static utility class");
  }

  /**
   * Create a burning status effect
   *
   * @param combatStats The combat stats component of the entity that burning is applied to.
   * @return The burning status effect.
   */
  public static StatusEffect createBurn(CombatStatsComponent combatStats) {
    return new Burning(1, 1000, 10000, combatStats);
  }

  /**
   * Create a regeneration status effect
   *
   * @param combatStats The combat stats component of the entity that regeneration is applied to.
   * @return The regeneration status effect.
   */
  public static StatusEffect createRegeneration(CombatStatsComponent combatStats) {
    return new Regeneration(1, 1000, 10000, combatStats);
  }

  /**
   * Create an invisibility status effect that starts counting down immediately.
   *
   * @param time the clock the countdown runs on.
   * @param duration how long the wearer stays hidden, in milliseconds.
   * @return The invisibility status effect.
   */
  public static TimedStatusEffect createInvisibility(GameTime time, long duration) {
    return new InvisibilityEffect(time, duration);
  }

  /**
   * Create a last stand status effect that starts counting down immediately.
   *
   * @param time the clock the countdown runs on.
   * @param duration how long the wearer stays amplified, in milliseconds.
   * @return The last stand status effect.
   */
  public static TimedStatusEffect createLastStand(GameTime time, long duration) {
    return new LastStandEffect(time, duration);
  }
}
