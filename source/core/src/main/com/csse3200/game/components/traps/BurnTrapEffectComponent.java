package com.csse3200.game.components.traps;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;

/** Applies the standard burning status effect when its trap is stepped on. */
public class BurnTrapEffectComponent extends TrapEffectComponent {
  @Override
  protected void applyEffect(
      StatusEffectsControllerComponent effects, CombatStatsComponent combatStats) {
    effects.addStatusEffect(StatusEffectsFactory.createBurn(combatStats));
  }
}
