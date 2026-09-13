package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

public class Shield implements StatusEffect {
  private static final int MAX_SHIELD = 20;
  private static final long ABSORB_DURATION_MS = 5000;
  private static final long TIMED_DURATION_MS = 3000;
  private static final long RECHARGE_DELAY_MS = 2000;
  private static final long RECHARGE_DURATION_MS = 7000;

  private long depletedAt = -1;

  private enum Mode {
    NONE,
    ABSORB,
    TIMED
  }

  private final GameTime time = new GameTime();

  private int current = MAX_SHIELD;
  private Mode mode = Mode.NONE;
  private long expiresAt = 0;

  public int getCurrent() {
    return current;
  }

  public int getMax() {
    return MAX_SHIELD;
  }

  public boolean isActive() {
    return mode != Mode.NONE;
  }

  public boolean activateAbsorb() {
    if (current < MAX_SHIELD || mode != Mode.NONE) {
    return false;
    }
    mode = Mode.ABSORB;
    expiresAt = time.getTime() + ABSORB_DURATION_MS;
    return true;
  }

  public boolean activateTimed() {
    if (current < MAX_SHIELD || mode != Mode.NONE) {
      return false;
    }

    current = 0;
    mode = Mode.TIMED;
    expiresAt = time.getTime() + TIMED_DURATION_MS;
    return true;
  }

  private void startRecharge() {
    if (depletedAt < 0) {
    depletedAt = time.getTime();
  }
}

  public int modifyIncomingDamage(int damage) {
    if (damage <= 0 || mode == Mode.NONE) {
      return damage;
    }

    if (mode == Mode.TIMED) {
      return 0;
    }

    int absorbed = Math.min(current, damage);
    current -= absorbed;

    if (current == 0) {
      mode = Mode.NONE;
      expiresAt = 0;
      startRecharge();
    }

    return damage - absorbed;
  }

  @Override
  public boolean update() {
    if (mode != Mode.NONE && time.getTime() >= expiresAt) {
      if (mode == Mode.ABSORB || mode == Mode.TIMED) {
        current = 0;
        startRecharge();
      }

      mode = Mode.NONE;
      expiresAt = 0;
    }
      if (mode == Mode.NONE && current < MAX_SHIELD && depletedAt >= 0) {
        long timeSinceDepleted = time.getTimeSince(depletedAt);
        
        if (timeSinceDepleted >= RECHARGE_DELAY_MS) {
          long rechargeTime = timeSinceDepleted - RECHARGE_DELAY_MS;
          long cappedRechargeTime = Math.min(rechargeTime, RECHARGE_DURATION_MS);
          current = (int) ((cappedRechargeTime * MAX_SHIELD) / RECHARGE_DURATION_MS);
          
          if (current >= MAX_SHIELD) {
            current = MAX_SHIELD;
            depletedAt = -1;
          }
        }
      }
    return false;
  }

  @Override
  public long getRemainingDuration() {
    if (mode == Mode.NONE) {
      return 0;
    }

    return Math.max(0, expiresAt - time.getTime());
  }
}