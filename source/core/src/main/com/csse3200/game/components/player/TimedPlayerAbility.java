package com.csse3200.game.components.player;

import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.services.GameTime;

/**
 * An ability that works by putting a timed status effect on the player. It owns an effect rather
 * than being one, so the status effects system runs the countdown and this class stays responsible
 * for the ability.
 */
public abstract class TimedPlayerAbility extends PlayerAbility {
  private final TimedStatusEffect effect;
  private StatusEffectsControllerComponent effects;

  protected TimedPlayerAbility(
      String name, GameTime time, long duration, long cooldown, boolean unlockedByDefault) {
    super(name, cooldown, unlockedByDefault);
    this.effect = new TimedStatusEffect(time, duration);
  }

  /** Returns how long one activation lasts, in milliseconds. */
  public long getDuration() {
    return effect.getDuration();
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
