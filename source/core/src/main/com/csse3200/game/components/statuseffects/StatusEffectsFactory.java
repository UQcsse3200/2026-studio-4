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
   * Creates a rechargeable shield status effect with the default shield configuration.
   *
   * @return a new shield status effect
   */
  public static Shield createShield() {
    return new Shield();
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

  /**
   * Create a slow status effect
   *
   * @param combatStats The combat stats component of the entity that slow is applied to.
   * @return The slow status effect.
   */
  public static StatusEffect createSlow(CombatStatsComponent combatStats) {
    return new Slow(10000, combatStats, -0.5f);
  }

  /**
   * Create a speed status effect
   *
   * @param combatStats The combat stats component of the entity that speed is applied to.
   * @return The speed status effect.
   */
  public static StatusEffect createSpeed(CombatStatsComponent combatStats) {
    return new Speed(10000, combatStats);
  }

  /**
   * Create a vulnerable status effect
   *
   * @return The vulnerable status effect
   */
  public static StatusEffect createVulnerable() {
    return new Vulnerable(5000);
  }

  /**
   * Create a new freeze status effect.
   *
   * @param combatStats The combat stats component of the entity that is frozen
   * @return the freeze status effect (a slow status effect that slows to 0)
   */
  public static StatusEffect createFreeze(CombatStatsComponent combatStats) {
    float speed = combatStats.getMovementSpeed();
    return new Slow(3000, combatStats, speed);
  }
}
