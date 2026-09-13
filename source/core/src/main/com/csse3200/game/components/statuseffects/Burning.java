package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.GameTime;

/** The object class for the burning status effect. */
public class Burning implements StatusEffect {
  private final GameTime time = new GameTime();

  private final int damage;
  private final long burnCooldown;
  private final long duration;
  private final long burnInit;
  private long lastBurn;

  private final CombatStatsComponent combatStats;

  /**
   * Create a new stack of burning.
   *
   * @param damage the amount of damage burning should do each time it activates.
   * @param burnCooldown the amount of time between instances of burn. in milliseconds.
   * @param duration the total time the burning lasts for.
   * @param combatStats the combat stats component of the entity which has the status effect.
   */
  public Burning(int damage, long burnCooldown, long duration, CombatStatsComponent combatStats) {
    this.damage = damage;
    this.burnCooldown = burnCooldown;
    this.duration = duration;
    this.combatStats = combatStats;
    burnInit = time.getTime();
  }

  /**
   * Updates the burn. Deals damage to the entity if the burn cooldown is completed.
   *
   * @return true if the burn status effect is ready for removal. false otherwise.
   */
  @Override
  public boolean update() {
    if (time.getTimeSince(lastBurn) > burnCooldown) {
      combatStats.takeDamage(damage);
      lastBurn = time.getTime();
    }
    return time.getTimeSince(burnInit) > duration;
  }

  /** Returns the time left until the status effect should be removed (in milliseconds). */
  public long getRemainingDuration() {
    return duration - time.getTimeSince(burnInit);
  }
}
