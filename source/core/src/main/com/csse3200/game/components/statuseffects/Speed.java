package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.GameTime;

/** The object class for the speed status effect. */
public class Speed implements StatusEffect {
  private final GameTime time = new GameTime();

  private final long duration;
  private final long speedInit;

  private final CombatStatsComponent combatStats;

  /**
   * Create a new stack of speed.
   *
   * @param duration the total time the speed lasts for.
   * @param combatStats the combat stats component of the entity which has the status effect.
   */
  public Speed(long duration, CombatStatsComponent combatStats) {
    this.duration = duration;
    this.combatStats = combatStats;
    speedInit = time.getTime();
    combatStats.addMovementSpeed(0.5f);
  }

  /**
   * Updates the speed. Removes buff if the time has elapsed.
   *
   * @return true if the speed status effect is ready for removal. false otherwise.
   */
  @Override
  public boolean update() {
    if (time.getTimeSince(speedInit) > duration) {
      combatStats.addMovementSpeed(-0.5f);
    }
    return time.getTimeSince(speedInit) > duration;
  }

  /** Returns the time left until the status effect should be removed (in milliseconds). */
  public long getRemainingDuration() {
    return duration - time.getTimeSince(speedInit);
  }
}
