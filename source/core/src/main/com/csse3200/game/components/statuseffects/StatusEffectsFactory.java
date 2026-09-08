package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.CombatStatsComponent;

/** Factory to create the status effects. */
public class StatusEffectsFactory {
  /**
   * Create a burning status effect
   *
   * @param combatStats The combat stats component of the entity that burning is applied to.
   * @return The burning status effect.
   */
  public static StatusEffect CreateBurn(CombatStatsComponent combatStats) {
    return new Burning(1, 1000, 10000, combatStats);
  }

  public static StatusEffect CreateRegeneration(CombatStatsComponent combatStats) {
    return new Regeneration(1, 1000, 10000, combatStats);
  }
}
