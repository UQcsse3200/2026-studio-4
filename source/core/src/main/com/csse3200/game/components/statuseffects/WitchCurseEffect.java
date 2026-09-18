package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

/** The small curse from witch bullet. It make walk slow and dash cannot work. */
public class WitchCurseEffect implements StatusEffect {
  public static final long DURATION = 3000L;
  private static final float MOVE_MULTIPLIER = 0.45f;

  private final GameTime time;
  private final long startTime;

  public WitchCurseEffect() {
    this(new GameTime());
  }

  WitchCurseEffect(GameTime time) {
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

  @Override
  public boolean disablesDash() {
    return true;
  }
}
