package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

/**
 * A rechargeable defensive status effect that protects an entity from incoming damage.
 *
 * <p>The shield supports two activation modes: absorb mode consumes shield points as damage is
 * received, while timed mode prevents all incoming damage for a short duration and immediately
 * depletes the shield. After depletion, the shield waits briefly before recharging to its maximum
 * capacity.
 */
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

  /**
   * @return the shield's current number of available points.
   */
  public int getCurrent() {
    return current;
  }

  /**
   * @return the shield's maximum number of points.
   */
  public int getMax() {
    return MAX_SHIELD;
  }

  /**
   * @return whether either shield activation mode is currently active.
   */
  public boolean isActive() {
    return mode != Mode.NONE;
  }

  /**
   * Activates absorb mode if the shield is full and not already active.
   *
   * <p>In absorb mode, incoming damage is deducted from the shield until its points are depleted or
   * the five-second duration expires.
   *
   * @return {@code true} if absorb mode was activated; {@code false} if activation was rejected
   */
  public boolean activateAbsorb() {
    if (current < MAX_SHIELD || mode != Mode.NONE) {
      return false;
    }
    mode = Mode.ABSORB;
    expiresAt = time.getTime() + ABSORB_DURATION_MS;
    return true;
  }

  /**
   * Activates timed mode if the shield is full and not already active.
   *
   * <p>Timed mode prevents all incoming damage for three seconds, then starts the shield recharge
   * process.
   *
   * @return {@code true} if timed mode was activated; {@code false} if activation was rejected
   */
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

  /**
   * Applies the active shield mode to incoming damage.
   *
   * <p>Absorb mode subtracts available shield points from the damage. Timed mode blocks the damage
   * completely. If the shield is inactive, or the damage is non-positive, the original value is
   * returned unchanged.
   *
   * @param damage the incoming damage amount
   * @return the damage remaining after shield mitigation
   */
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

  /**
   * Advances the shield timer and recharge state.
   *
   * <p>When an activation expires, the shield is depleted. Once the recharge delay has elapsed,
   * shield points are restored gradually over the configured recharge duration.
   *
   * @return always {@code false}; the shield remains registered as a status effect
   */
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

  /**
   * Returns the time remaining for the current activation.
   *
   * @return activation time remaining in milliseconds, or {@code 0} when inactive
   */
  @Override
  public long getRemainingDuration() {
    if (mode == Mode.NONE) {
      return 0;
    }

    return Math.max(0, expiresAt - time.getTime());
  }
}
