package com.csse3200.game.components.player;

import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import java.util.function.Supplier;

/**
 * An ability whose whole run is one timed condition on the player. Each start creates a fresh
 * condition and hands it to the player's status effects controller, which owns the countdown, and
 * every question about whether the ability is running is answered from that.
 *
 * <p>Subclasses supply how to make the condition and say what makes the ability start: a cast
 * ability overrides {@link #isCastable()}, a passive overrides {@link #triggersOnDamage}. Neither
 * has to repeat the wiring. An ability that finishes the moment it starts, or that acts on somebody
 * other than the player, extends {@link PlayerAbility} directly instead and never touches this.
 */
public abstract class TimedPlayerAbility extends PlayerAbility {
  private final Supplier<TimedStatusEffect> effectFactory;
  private StatusEffectsControllerComponent effects;
  private Runnable onEnded;
  private TimedStatusEffect current;

  /**
   * @param name name carried by the abilityUsed, abilityEnded and abilityFailed events
   * @param cooldown wait before the ability may start again, measured from the start, in ms
   * @param unlockedByDefault whether the ability is usable without being unlocked first
   * @param effectFactory makes the condition this ability puts on the player, once per start
   */
  protected TimedPlayerAbility(
      String name,
      long cooldown,
      boolean unlockedByDefault,
      Supplier<TimedStatusEffect> effectFactory) {
    super(name, cooldown, unlockedByDefault);
    this.effectFactory = effectFactory;
  }

  /** Returns the condition currently on the player, or null when the ability is not running. */
  protected TimedStatusEffect getEffect() {
    return isRunning() ? current : null;
  }

  @Override
  protected void attach(StatusEffectsControllerComponent controller, Runnable onEnded) {
    this.effects = controller;
    this.onEnded = onEnded;
  }

  @Override
  public void start() {
    if (effects == null) {
      return;
    }
    // Restarting mid-run replaces the old condition so only one countdown ends this ability.
    stop();
    TimedStatusEffect effect = effectFactory.get();
    current = effect;
    effect.setOnEnded(
        () -> {
          if (current == effect) {
            current = null;
          }
          if (onEnded != null) {
            onEnded.run();
          }
        });
    effects.addStatusEffect(effect);
  }

  /** Routes through the controller, so the end callback keeps its usual ordering. */
  @Override
  public void stop() {
    if (effects != null && current != null) {
      effects.removeStatusEffect(current);
    }
  }

  @Override
  public boolean isRunning() {
    return effects != null && effects.hasStatusEffect(current);
  }

  @Override
  public long getRemainingMs() {
    return isRunning() ? current.getRemainingDuration() : 0;
  }
}
