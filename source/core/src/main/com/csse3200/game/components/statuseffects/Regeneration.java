package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.GameTime;

/** The object class for the burning status effect. */
public class Regeneration implements StatusEffect {
  private final GameTime time = new GameTime();

  private final int healing;
  private final long HEAL_COOLDOWN;
  private final long duration;
  private final long healInit;
  private long lastBurn;

  private final CombatStatsComponent combatStats;

  /**
   * Create a new stack of regeneration.
   *
   * @param healing the amount of healing regeneration should do each time it activates.
   * @param HEAL_COOLDOWN the amount of time between instances of regeneration. in milliseconds.
   * @param duration the total time the regeneration lasts for.
   * @param combatStats the combat stats component of the entity which has the status effect.
   */
  public Regeneration(
      int healing, long HEAL_COOLDOWN, long duration, CombatStatsComponent combatStats) {
    this.healing = healing;
    this.HEAL_COOLDOWN = HEAL_COOLDOWN;
    this.duration = duration;
    this.combatStats = combatStats;
    healInit = time.getTime();
  }

  /**
   * Updates the regeneration. Heals the entity if the heal cooldown is completed.
   *
   * @return true if the heal status effect is ready for removal. false otherwise.
   */
  @Override
  public boolean update() {
    if (time.getTimeSince(lastBurn) > HEAL_COOLDOWN) {
      combatStats.addHealth(healing);
      lastBurn = time.getTime();
    }
    if (time.getTimeSince(healInit) > duration) {
      return true;
    }
    return false;
  }

  /** Returns the time left until the status effect should be removed (in milliseconds). */
  public long getRemainingDuration() {
    return duration - time.getTimeSince(healInit);
  }
}
