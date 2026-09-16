package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.Damage;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.Damageable;
import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.EnumMap;
import java.util.Map;

/**
 * Owns consumable use: validates, removes exactly one item, applies its effect, then emits
 * itemUsed. Input requests use through tryUse or useConsumable; itemUsed is a success notification
 * only.
 */
public class ConsumableEffectComponent extends Component {
  public static final String USE_REQUEST = "useConsumable";
  public static final String USED = "itemUsed";
  public static final long DURATION_MS = 8000;
  private final Map<ItemType, TimedStatusEffect> active = new EnumMap<>(ItemType.class);
  private CombatStatsComponent stats;
  private InventoryComponent inventory;
  private StatusEffectsControllerComponent effects;
  private GameTime time;
  private boolean disposed;

  @Override
  public void create() {
    stats = entity.getComponent(CombatStatsComponent.class);
    inventory = entity.getComponent(InventoryComponent.class);
    effects = entity.getComponent(StatusEffectsControllerComponent.class);
    time = ServiceLocator.getTimeSource();
    entity.getEvents().addListener(USE_REQUEST, this::applyEffect);
  }

  /** Event-compatible use request. This method never assumes the caller already removed an item. */
  public void applyEffect(ItemType type) {
    tryUse(type);
  }

  /**
   * Applies one available item. Invalid, dead, disposed or full-health requests consume nothing.
   */
  public boolean tryUse(ItemType type) {
    if (disposed
        || type == null
        || !type.isConsumable()
        || stats == null
        || stats.isDead()
        || inventory == null
        || !inventory.hasConsumable(type)) {
      return false;
    }
    if (type == ItemType.HEALTH_POTION) {
      if (stats.getHealth() >= stats.getMaxHealth()) {
        return false;
      }
    } else if (effects == null || effects.isDisposed() || time == null) {
      return false;
    }
    if (!inventory.removeConsumable(type)) {
      return false;
    }
    if (type == ItemType.HEALTH_POTION) {
      stats.addHealth(25);
    } else {
      refresh(type);
    }
    entity.getEvents().trigger(USED, type);
    return true;
  }

  private void refresh(ItemType type) {
    effects.removeStatusEffect(active.remove(type));
    TimedStatusEffect effect = new ConsumableBuff(time, type);
    active.put(type, effect);
    effect.setOnEnded(
        () -> {
          active.remove(type, effect);
          stats.notifyEffectiveStatsChanged();
        });
    effects.addStatusEffect(effect);
    stats.notifyEffectiveStatsChanged();
  }

  /** Returns whether this consumable's protection is still active at the current game time. */
  public boolean isShielded() {
    return effects != null && effects.hasStatusEffect(active.get(ItemType.SHIELD));
  }

  /** Remaining duration of the actual active consumable shield, in milliseconds. */
  public long getShieldRemainingMs() {
    return getRemainingMs(ItemType.SHIELD);
  }

  /** Remaining duration of a real active consumable effect; zero when absent or removed. */
  public long getRemainingMs(ItemType type) {
    TimedStatusEffect effect = type == null ? null : active.get(type);
    return effects != null && effect != null && effects.hasStatusEffect(effect)
        ? effect.getRemainingDuration()
        : 0;
  }

  @Override
  public void dispose() {
    disposed = true;
    if (effects != null) {
      for (TimedStatusEffect effect : active.values().toArray(TimedStatusEffect[]::new)) {
        effects.removeStatusEffect(effect);
      }
    }
    active.clear();
  }

  /** Refreshable consumable modifiers use the shared status and damage interfaces. */
  private static final class ConsumableBuff extends TimedStatusEffect implements Damageable {
    private final ItemType type;

    ConsumableBuff(GameTime time, ItemType type) {
      super(time, DURATION_MS);
      this.type = type;
    }

    @Override
    public float getStatMultiplier(Stat stat) {
      return (type == ItemType.STRENGTH_POTION && stat == Stat.ATTACK)
              || (type == ItemType.SPEED_POTION && stat == Stat.MOVEMENT_SPEED)
          ? 1.5f
          : 1f;
    }

    @Override
    public boolean damage(Damage damage) {
      if (type == ItemType.SHIELD && !isExpired()) {
        damage.setDamage(0);
      }
      return false;
    }
  }
}
