package com.csse3200.game.components.player.abilities;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.PlayerAbility;
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
public final class LastStand extends PlayerAbility {
  /** Name carried by the abilityUsed and abilityEnded events. */
  public static final String NAME = "laststand";

  public static final long DURATION_MS = 10_000;
  public static final long COOLDOWN_MS = 60_000;

  /** Applied to effective strength, movement speed and attack speed while the effect runs. */
  public static final float MULTIPLIER = 1.5f;

  /** Share of max health at or below which a hostile hit applies the effect. */
  private static final int HEALTH_PERCENT = 20;

  private final LastStandEffect effect;
  private StatusEffectsControllerComponent effects;

  public LastStand(GameTime time) {
    super(NAME, COOLDOWN_MS, false);
    effect = new LastStandEffect(time, DURATION_MS);
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

  @Override
  protected void attach(StatusEffectsControllerComponent controller, Runnable onEnded) {
    effects = controller;
    effect.setOnEnded(onEnded);
    controller.registerEffect(effect);
  }

  @Override
  public void start() {
    effect.activate();
  }

  /** Routes through the controller, so the end callback keeps its usual ordering. */
  @Override
  public void stop() {
    if (effects != null) {
      effects.removeEffect(effect);
    }
  }

  @Override
  public boolean isRunning() {
    return effect.isActive();
  }

  @Override
  public long getRemainingMs() {
    return effect.getRemainingDuration();
  }

  /** Returns whether the amplifier is running on an entity. */
  public static boolean isActiveOn(Entity target) {
    return isRunningOn(target, LastStand.class);
  }
}
