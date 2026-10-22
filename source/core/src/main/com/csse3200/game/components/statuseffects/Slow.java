package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.GameTime;

/** The object class for the slow status effect. */
public class Slow extends StatusEffect {

  private final long duration;
  private final long slowInit;
  private final float appliedAmount;
  private final CombatStatsComponent combatStats;
  private boolean removed;

  /**
   * Create a new stack of slow.
   *
   * @param duration the total time the slow lasts for.
   * @param combatStats the combat stats component of the entity which has the status effect.
   * @param amount the movement speed adjustment, clamped so speed cannot become negative.
   */
  public Slow(long duration, CombatStatsComponent combatStats, float amount) {
    super(new GameTime(), duration);
    this.duration = duration;
    this.combatStats = combatStats;
    slowInit = time.getTime();
    appliedAmount = Math.max(amount, -combatStats.getMovementSpeed());
    combatStats.addMovementSpeed(appliedAmount);
  }

  /**
   * Updates the slow. Removes the debuff if the time has elapsed.
   *
   * @return true if the slow status effect is ready for removal. false otherwise.
   */
  @Override
  public boolean update() {
    return time.getTimeSince(slowInit) > duration;
  }

  /** Restores only this effect's speed adjustment when it expires or is removed early. */
  @Override
  public void onRemoved() {
    if (removed) {
      return;
    }
    removed = true;
    combatStats.addMovementSpeed(-appliedAmount);
    super.onRemoved();
  }

  /** Returns the time left until the status effect should be removed (in milliseconds). */
  @Override
  public long getRemainingDuration() {
    return duration - time.getTimeSince(slowInit);
  }
}
