package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

/** A reusable active deadline, expired in batches by the status effects controller. */
public abstract class TimedStatusEffect implements StatusEffect {
  private final GameTime time;
  private final long duration;
  private final Runnable onEnded;
  private long deadline;
  private boolean active;

  protected TimedStatusEffect(GameTime time, long duration, Runnable onEnded) {
    this.time = time;
    this.duration = duration;
    this.onEnded = onEnded;
  }

  public void activate() {
    deadline = time.getTime() + duration;
    active = true;
  }

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

  /** Clears state without callbacks so the controller can clear an entire batch first. */
  public void clear() {
    active = false;
    deadline = 0;
  }

  public void notifyEnded() {
    onEnded.run();
  }
}
