package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

/** A timed movement penalty with a red blink only while the penalty remains active. */
public class PetrificationEffect extends RedFlashEffect {
  private final float movementMultiplier;

  /**
   * @param time the status-effect clock
   * @param duration duration in milliseconds
   * @param movementMultiplier remaining movement-speed fraction, from zero to one
   */
  public PetrificationEffect(GameTime time, long duration, float movementMultiplier) {
    super(time, duration);
    this.movementMultiplier = movementMultiplier;
  }

  @Override
  public float getStatMultiplier(Stat stat) {
    return stat == Stat.MOVEMENT_SPEED ? movementMultiplier : 1f;
  }
}
