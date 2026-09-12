package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

/** Active lifetime of hostile immunity and undetectability; does not change raw combat stats. */
public final class Invisibility extends TimedStatusEffect {
  public Invisibility(GameTime time, long duration, Runnable onEnded) {
    super(time, duration, onEnded);
  }
}
