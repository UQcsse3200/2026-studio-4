package com.csse3200.game.components.statuseffects;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.services.GameTime;

/** A timed red blink that leaves transparency and the owner's gameplay stats unchanged. */
public class RedFlashEffect extends TimedStatusEffect {
  private static final long BLINK_HALF_PERIOD_MILLIS = 250L;
  private static final Color RED_TINT = new Color(1f, 0.2f, 0.2f, 1f);

  /**
   * @param time the shared gameplay clock
   * @param duration total blink duration in milliseconds
   */
  public RedFlashEffect(GameTime time, long duration) {
    super(time, duration);
  }

  @Override
  public Color getTint() {
    long remaining = getRemainingDuration();
    if (remaining == 0L) {
      return null;
    }
    long elapsed = getDuration() - remaining;
    return (elapsed / BLINK_HALF_PERIOD_MILLIS) % 2L == 0L ? RED_TINT : null;
  }
}
