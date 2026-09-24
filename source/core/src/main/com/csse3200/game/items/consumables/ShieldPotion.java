package com.csse3200.game.items.consumables;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Damage;
import com.csse3200.game.components.statuseffects.Damageable;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.services.GameTime;

/** Blocks incoming damage for a fixed duration. */
public final class ShieldPotion extends ConsumableItem {
  private final long durationMs;

  public ShieldPotion(ItemType type, int quantity, long durationMs) {
    super(type, quantity);
    this.durationMs = durationMs;
  }

  @Override
  public TimedStatusEffect use(CombatStatsComponent stats, GameTime time) {
    return new ShieldEffect(time, durationMs);
  }

  private static final class ShieldEffect extends TimedStatusEffect implements Damageable {
    ShieldEffect(GameTime time, long durationMs) {
      super(time, durationMs);
    }

    @Override
    public boolean damage(Damage damage) {
      if (!isExpired()) {
        damage.setDamage(0);
      }
      return false;
    }
  }
}
