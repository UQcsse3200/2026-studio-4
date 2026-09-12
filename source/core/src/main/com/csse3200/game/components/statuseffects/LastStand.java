package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

/** Active lifetime of the effective strength, movement-speed and attack-speed boost. */
public final class LastStand extends TimedStatusEffect {
  public LastStand(GameTime time, long duration, Runnable onEnded) {
    super(time, duration, onEnded);
  }
}
