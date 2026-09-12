package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

/**
 * A status effect that simply runs for a fixed time on whoever it is put on.
 *
 * <p>Anything that needs a countdown on an entity creates one of these and hands it to that
 * entity's status effects controller, which runs the countdown. It does not care who created it or
 * which entity it landed on, so an effect a player puts on an enemy works exactly like one they put
 * on themselves.
 */
public class TimedStatusEffect implements TimedEffect {
  private final GameTime time;
  private final long duration;
  private Runnable onEnded;
  private long deadline;
  private boolean active;

  public TimedStatusEffect(GameTime time, long duration) {
    this.time = time;
    this.duration = duration;
  }

  /** Sets the callback run once, after the effect expires or is removed. */
  public void setOnEnded(Runnable onEnded) {
    this.onEnded = onEnded;
  }

  /** Returns how long a single activation lasts, in milliseconds. */
  public long getDuration() {
    return duration;
  }

  /** Starts the countdown, or restarts it from full if it is already running. */
  public void activate() {
    deadline = time.getTime() + duration;
    active = true;
  }

  @Override
  public boolean isActive() {
    return active;
  }

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
