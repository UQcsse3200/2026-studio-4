package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.GameTime;

/** The object class for the slow status effect. */
public class Slow implements StatusEffect {
  private final GameTime time = new GameTime();

  private final long duration;
  private final long slowInit;

  private final CombatStatsComponent combatStats;

  /**
   * Create a new stack of slow.
   *
   * @param duration the total time the slow lasts for.
   * @param combatStats the combat stats component of the entity which has the status effect.
   */
  public Slow(long duration, CombatStatsComponent combatStats) {
    this.duration = duration;
    this.combatStats = combatStats;
    slowInit = time.getTime();
    combatStats.addMovementSpeed(-0.5f);
  }

  /**
   * Updates the slow. Removes the debuff if the time has elapsed.
   *
   * @return true if the slow status effect is ready for removal. false otherwise.
   */
  @Override
  public boolean update() {
    if (time.getTimeSince(slowInit) > duration) {
      combatStats.addMovementSpeed(0.5f);
    }
    return time.getTimeSince(slowInit) > duration;
  }

  /** Returns the time left until the status effect should be removed (in milliseconds). */
  public long getRemainingDuration() {
    return duration - time.getTimeSince(slowInit);
  }
}
