package com.csse3200.game.components.traps;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.services.ServiceLocator;

/** Applies the standard frozen status effect when its trap is stepped on. */
public class FreezeTrapEffectComponent extends TrapEffectComponent {
  /** Matches the player's freeze spell duration. */
  public static final long DURATION_MILLIS = 5_000L;

  @Override
  protected void applyEffect(
      StatusEffectsControllerComponent effects, CombatStatsComponent combatStats) {
    effects.addStatusEffect(
        StatusEffectsFactory.createFrozen(ServiceLocator.getTimeSource(), DURATION_MILLIS));
  }
}
