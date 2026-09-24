package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

/** The small curse from a wizard projectile. It moderately slows movement. */
public class WizardCurseEffect implements StatusEffect {
  public static final long DURATION = 3000L;
  private static final float MOVE_MULTIPLIER = 0.725f;

  private final GameTime time;
  private final long startTime;

  public WizardCurseEffect() {
    this(new GameTime());
  }

  WizardCurseEffect(GameTime time) {
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
  public float getStatMultiplier(Stat stat) {
    if (stat == Stat.MOVEMENT_SPEED) {
      return MOVE_MULTIPLIER;
    }
    return 1f;
  }
}
