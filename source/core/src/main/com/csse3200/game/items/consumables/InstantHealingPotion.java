package com.csse3200.game.items.consumables;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.services.GameTime;

/** Restores a fixed amount of health immediately. */
public final class InstantHealingPotion extends ConsumableItem {
  private final int healing;

  public static InstantHealingPotion small(int quantity) {
    return new InstantHealingPotion(quantity, 25);
  }

  public static InstantHealingPotion medium(int quantity) {
    return new InstantHealingPotion(quantity, 50);
  }

  public static InstantHealingPotion large(int quantity) {
    return new InstantHealingPotion(quantity, 100);
  }

  public InstantHealingPotion(int quantity, int healing) {
    super(
        idFor(healing),
        nameFor(healing),
        "Restores " + healing + " health when consumed.",
        "images/health_potion_pixel.png",
        quantity);
    this.healing = healing;
  }

  private static String idFor(int healing) {
    return switch (healing) {
      case 25 -> ItemIds.HEALTH_POTION;
      case 50 -> ItemIds.MEDIUM_HEALTH_POTION;
      case 100 -> ItemIds.LARGE_HEALTH_POTION;
      default -> throw new IllegalArgumentException("Unsupported healing amount: " + healing);
    };
  }

  private static String nameFor(int healing) {
    return switch (healing) {
      case 25 -> "Small Health Potion";
      case 50 -> "Medium Health Potion";
      case 100 -> "Large Health Potion";
      default -> throw new IllegalArgumentException("Unsupported healing amount: " + healing);
    };
  }

  @Override
  public boolean canUse(
      CombatStatsComponent stats, StatusEffectsControllerComponent effects, GameTime time) {
    return stats.getHealth() < stats.getMaxHealth();
  }

  @Override
  public TimedStatusEffect use(CombatStatsComponent stats, GameTime time) {
    stats.addHealth(healing);
    return null;
  }
}
