package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

/** Reverses movement controls and swaps the player's attack and dash keys for three seconds. */
public class ConfusionEffect implements StatusEffect {
  public static final long DURATION = 3000L;

  private final GameTime time;
  private final long startTime;

  public ConfusionEffect() {
    this(new GameTime());
  }

  ConfusionEffect(GameTime time) {
    this.time = time;
    startTime = time.getTime();
  }

  @Override
  public boolean update() {
    return isExpired();
  }

  @Override
  public long getRemainingDuration() {
    return DURATION - time.getTimeSince(startTime);
  }

  @Override
  public boolean confusesControls() {
    return true;
  }
}
