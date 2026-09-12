package com.csse3200.game.components.player;

import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.TimedEffect;
import com.csse3200.game.services.GameTime;

/**
 * An ability that puts a timed condition on the player themselves, so the status effects controller
 * runs the countdown and this class stays responsible for the ability.
 *
 * <p>Only this subset of abilities is a status effect. An ability that finishes at once, or that
 * puts its condition on somebody else such as an enemy, extends PlayerAbility instead and applies
 * whatever effects it likes through the controller on that entity.
 */
public abstract class TimedPlayerAbility extends PlayerAbility implements TimedEffect {
  private final GameTime time;
  private final long duration;

  private StatusEffectsControllerComponent effects;
  private Runnable onEnded;
  private long deadline;
  private boolean active;

  protected TimedPlayerAbility(
      String name, GameTime time, long duration, long cooldown, boolean unlockedByDefault) {
    super(name, cooldown, unlockedByDefault);
    this.time = time;
    this.duration = duration;
  }

  /** Returns how long one activation lasts, in milliseconds. */
  public long getDuration() {
    return duration;
  }

  @Override
  protected void attach(StatusEffectsControllerComponent controller, Runnable ended) {
    effects = controller;
    onEnded = ended;
    controller.registerEffect(this);
  }

  @Override
  public void start() {
    deadline = time.getTime() + duration;
    active = true;
  }

  /** Routes through the controller, so the end callback keeps its usual ordering. */
  @Override
  public void stop() {
    if (effects != null) {
      effects.removeEffect(this);
    }
  }

  /** A timed ability runs for exactly as long as the condition it puts on the player. */
  @Override
  public boolean isRunning() {
    return isActive();
  }

  @Override
  public long getRemainingMs() {
    return getRemainingDuration();
  }

  @Override
  public boolean isActive() {
    return active;
  }

  /** Returns whether the deadline has passed, which is what marks the effect for expiry. */
  @Override
  public boolean update() {
    return active && time.getTime() >= deadline;
  }

  @Override
  public long getRemainingDuration() {
    return active ? Math.max(0, deadline - time.getTime()) : 0;
  }

  @Override
  public void clear() {
    active = false;
    deadline = 0;
  }

  @Override
  public void notifyEnded() {
    if (onEnded != null) {
      onEnded.run();
    }
  }
}
