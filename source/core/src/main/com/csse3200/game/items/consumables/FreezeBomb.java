package com.csse3200.game.items.consumables;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.spells.targeting.StrategyOnscreen;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/** Freezes enemies visible through the current world camera when consumed. */
public final class FreezeBomb extends ConsumableItem {
  private final long durationMs;

  public FreezeBomb(ItemType type, int quantity, long durationMs) {
    super(type, quantity);
    if (durationMs <= 0) {
      throw new IllegalArgumentException("durationMs must be positive");
    }
    this.durationMs = durationMs;
  }

  @Override
  public boolean canUse(
      CombatStatsComponent stats, StatusEffectsControllerComponent effects, GameTime time) {
    return super.canUse(stats, effects, time)
        && stats.getEntity() != null
        && ServiceLocator.getWorldCamera() != null
        && ServiceLocator.getEntityService() != null;
  }

  @Override
  public TimedStatusEffect use(CombatStatsComponent stats, GameTime time) {
    for (Entity enemy :
        new StrategyOnscreen(ServiceLocator.getWorldCamera()).selectTargets(stats.getEntity())) {
      StatusEffectsControllerComponent effects =
          enemy.getComponent(StatusEffectsControllerComponent.class);
      if (effects != null && !effects.isDisposed()) {
        effects.addStatusEffect(new FrozenEffect(time, durationMs));
      }
    }
    if (ServiceLocator.getRenderService() != null) {
      ServiceLocator.getRenderService().startWhiteFlash();
    }
    return null;
  }
}
