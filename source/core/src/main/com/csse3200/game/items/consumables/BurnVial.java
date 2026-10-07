package com.csse3200.game.items.consumables;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.BurnVialFeedbackComponent;
import com.csse3200.game.components.spells.targeting.StrategyOnscreen;
import com.csse3200.game.components.statuseffects.Burning;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Sets enemies visible through the current world camera alight when consumed.
 *
 * <p>Reuses the existing {@link Burning} status effect rather than a new damage-over-time system,
 * applying one stack per onscreen enemy the same way {@link FreezeBomb} applies {@code
 * FrozenEffect}.
 */
public final class BurnVial extends ConsumableItem {
  private static final int BURN_DAMAGE = 2;
  private static final long BURN_COOLDOWN_MS = 1000;
  private static final long DEFAULT_DURATION_MS = 6000;
  private final long durationMs;

  public BurnVial(int quantity) {
    this(quantity, DEFAULT_DURATION_MS);
  }

  public BurnVial(int quantity, long durationMs) {
    super(
        ItemIds.BURN_VIAL,
        "Burn Vial",
        "Sets all enemies currently on screen alight for "
            + formatAmount(durationMs / 1000f)
            + " seconds.",
        "images/burn_vial_pixel.png",
        quantity);
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
    BurnVialFeedbackComponent feedback =
        stats.getEntity().getComponent(BurnVialFeedbackComponent.class);
    for (Entity enemy :
        new StrategyOnscreen(ServiceLocator.getWorldCamera()).selectTargets(stats.getEntity())) {
      StatusEffectsControllerComponent effects =
          enemy.getComponent(StatusEffectsControllerComponent.class);
      CombatStatsComponent enemyStats = enemy.getComponent(CombatStatsComponent.class);
      if (effects != null && !effects.isDisposed() && enemyStats != null) {
        VialBurning burn = new VialBurning(BURN_DAMAGE, BURN_COOLDOWN_MS, durationMs, enemyStats);
        effects.addStatusEffect(burn);
        if (feedback != null) feedback.trackBurn(enemy, burn);
      }
    }
    if (ServiceLocator.getRenderService() != null) {
      ServiceLocator.getRenderService().startFireFlash();
    }
    return null;
  }
}
