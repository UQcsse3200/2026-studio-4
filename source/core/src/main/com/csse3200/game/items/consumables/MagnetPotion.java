package com.csse3200.game.items.consumables;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.MagnetEffect;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Pulls nearby Gold and consumables towards the player for a short time when consumed.
 *
 * <p>The pull itself lives in {@link MagnetEffect}, a timed status effect on the player, so the
 * existing {@code ConsumableEffectComponent} handles its countdown, refreshing on re-use, and
 * cleanup exactly as it does for the Speed and Strength potions.
 */
public final class MagnetPotion extends ConsumableItem {
  public static final long DEFAULT_DURATION_MS = 10000;
  public static final float RADIUS = 4f;
  public static final float PULL_SPEED = 6f;
  public static final float COLLECT_DISTANCE = 0.4f;

  private final long durationMs;

  public MagnetPotion(int quantity) {
    this(quantity, DEFAULT_DURATION_MS);
  }

  public MagnetPotion(int quantity, long durationMs) {
    super(
        ItemIds.MAGNET_POTION,
        "Magnet Potion",
        "Pulls nearby Gold and consumables towards you for "
            + formatAmount(durationMs / 1000f)
            + " seconds.",
        "images/magnet_potion_pixel.png",
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
        && ServiceLocator.getEntityService() != null;
  }

  @Override
  public TimedStatusEffect use(CombatStatsComponent stats, GameTime time) {
    return new MagnetEffect(
        time, durationMs, stats.getEntity(), RADIUS, PULL_SPEED, COLLECT_DISTANCE);
  }
}
