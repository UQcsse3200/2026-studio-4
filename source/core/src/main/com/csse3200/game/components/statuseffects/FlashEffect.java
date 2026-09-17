package com.csse3200.game.components.statuseffects;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.services.GameTime;

/**
 * A timed blink in one colour that leaves transparency and the owner's gameplay stats unchanged.
 *
 * <p>The blink is derived from the time remaining rather than driven by a timer of its own, so a
 * flash short enough to cover only the first half period simply shows once and ends.
 */
public class FlashEffect extends TimedStatusEffect {
  private static final long BLINK_HALF_PERIOD_MILLIS = 250L;

  private final Color tint;
  private final Color glow;

  /**
   * A blink that tints only, which leaves a dark sprite looking unchanged.
   *
   * @param time the shared gameplay clock
   * @param duration total blink duration in milliseconds
   * @param tint colour multiplied into the owner's sprite while the blink is on
   */
  public FlashEffect(GameTime time, long duration, Color tint) {
    this(time, duration, tint, null);
  }

  /**
   * @param time the shared gameplay clock
   * @param duration total blink duration in milliseconds
   * @param tint colour multiplied into the owner's sprite while the blink is on
   * @param glow colour added on top while the blink is on, so the blink shows on a dark sprite too;
   *     null to tint only
   */
  public FlashEffect(GameTime time, long duration, Color tint, Color glow) {
    super(time, duration);
    this.tint = tint;
    this.glow = glow;
  }

  @Override
  public Color getTint() {
    return isBlinkOn() ? tint : null;
  }

  @Override
  public Color getGlow() {
    return isBlinkOn() ? glow : null;
  }

  /** Both halves of the look come and go together, so they are driven by the one phase. */
  private boolean isBlinkOn() {
    long remaining = getRemainingDuration();
    if (remaining == 0L) {
      return false;
    }
    long elapsed = getDuration() - remaining;
    return (elapsed / BLINK_HALF_PERIOD_MILLIS) % 2L == 0L;
  }
}
