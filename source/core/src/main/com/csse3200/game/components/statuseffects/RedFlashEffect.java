package com.csse3200.game.components.statuseffects;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.services.GameTime;

/** A timed red blink that leaves transparency and the owner's gameplay stats unchanged. */
public class RedFlashEffect extends FlashEffect {
  private static final Color RED_TINT = new Color(1f, 0.2f, 0.2f, 1f);

  /**
   * @param time the shared gameplay clock
   * @param duration total blink duration in milliseconds
   */
  public RedFlashEffect(GameTime time, long duration) {
    super(time, duration, RED_TINT);
  }
}
