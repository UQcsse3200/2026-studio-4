package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

/**
 * A status effect that simply runs for a fixed time from the moment it is created, on whoever it is
 * put on.
 *
 * <p>Anything that needs a countdown on an entity creates one of these and hands it to that
 * entity's status effects controller, which runs the countdown. It does not care who created it or
 * which entity it landed on, so an effect a player puts on an enemy works exactly like one they put
 * on themselves. A fresh instance is made per application, the same as a burn stack.
 */
public class TimedStatusEffect implements StatusEffect {
  private final GameTime time;
  private final long duration;
  private final long deadline;
  private Runnable onEnded;

  public TimedStatusEffect(GameTime time, long duration) {
    this.time = time;
    this.duration = duration;
    this.deadline = time.getTime() + duration;
  }

  /** Sets the callback run once, after the effect expires or is removed. */
  public void setOnEnded(Runnable onEnded) {
    this.onEnded = onEnded;
  }

  /** Returns how long the effect lasts from creation, in milliseconds. */
  public long getDuration() {
    return duration;
  }

  @Override
  public boolean update() {
    return isExpired();
  }

  @Override
  public long getRemainingDuration() {
    return Math.max(0, deadline - time.getTime());
  }

  @Override
  public void onRemoved() {
    if (onEnded != null) {
      onEnded.run();
    }
  }
}
