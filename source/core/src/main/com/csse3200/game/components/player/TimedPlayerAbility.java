package com.csse3200.game.components.player;

import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;

/**
 * An ability whose whole run is one timed condition on the player. It hands that condition to the
 * player's status effects controller, which owns the countdown, and answers every question about
 * whether the ability is running from it.
 *
 * <p>Subclasses supply the condition and say what makes the ability start: a cast ability overrides
 * {@link #isCastable()}, a passive overrides {@link #triggersOnDamage}. Neither has to repeat the
 * wiring. An ability that finishes the moment it starts, or that acts on somebody other than the
 * player, extends {@link PlayerAbility} directly instead and never touches this.
 */
public abstract class TimedPlayerAbility extends PlayerAbility {
  private final TimedStatusEffect effect;
  private StatusEffectsControllerComponent effects;

  /**
   * @param name name carried by the abilityUsed, abilityEnded and abilityFailed events
   * @param cooldown wait before the ability may start again, measured from the start, in ms
   * @param unlockedByDefault whether the ability is usable without being unlocked first
   * @param effect the condition this ability puts on the player while it runs
   */
  protected TimedPlayerAbility(
      String name, long cooldown, boolean unlockedByDefault, TimedStatusEffect effect) {
    super(name, cooldown, unlockedByDefault);
    this.effect = effect;
  }

  /** Returns the condition this ability puts on the player, for subclasses that need to read it. */
  protected TimedStatusEffect getEffect() {
    return effect;
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
}
