package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashMap;
import java.util.Map;

/**
 * Owns consumable use: validates, removes exactly one item, applies its effect, then emits
 * itemUsed. Input requests use through tryUse or useConsumable; itemUsed is a success notification
 * only.
 */
public class ConsumableEffectComponent extends Component {
  public static final String USE_REQUEST = "useConsumable";
  public static final String USED = "itemUsed";
  private final Map<String, TimedStatusEffect> active = new HashMap<>();
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
  public void applyEffect(String id) {
    tryUse(id);
  }

  /**
   * Applies one available item. Invalid, dead, disposed or full-health requests consume nothing.
   */
  public boolean tryUse(String id) {
    if (disposed
        || id == null
        || !ItemCatalog.contains(id)
        || stats == null
        || stats.isDead()
        || inventory == null
        || !inventory.hasConsumable(id)) {
      return false;
    }
    if (!(ItemCatalog.create(id, 1) instanceof ConsumableItem item)) {
      return false;
    }
    if (!item.canUse(stats, effects, time)) {
      return false;
    }
    if (!inventory.removeConsumable(id)) {
      return false;
    }
    TimedStatusEffect effect = item.use(stats, time);
    if (effect != null) {
      refresh(id, effect);
    }
    entity.getEvents().trigger(USED, id);
    return true;
  }

  private void refresh(String id, TimedStatusEffect effect) {
    effects.removeStatusEffect(active.remove(id));
    active.put(id, effect);
    effect.setOnEnded(
        () -> {
          active.remove(id, effect);
          stats.notifyEffectiveStatsChanged();
        });
    effects.addStatusEffect(effect);
    stats.notifyEffectiveStatsChanged();
  }

  /** Returns whether this consumable's protection is still active at the current game time. */
  public boolean isShielded() {
    return effects != null && effects.hasStatusEffect(active.get(ItemIds.SHIELD));
  }

  /** Remaining duration of the actual active consumable shield, in milliseconds. */
  public long getShieldRemainingMs() {
    return getRemainingMs(ItemIds.SHIELD);
  }

  /** Remaining duration of a real active consumable effect; zero when absent or removed. */
  public long getRemainingMs(String id) {
    TimedStatusEffect effect = id == null ? null : active.get(id);
    return effects != null && effect != null && effects.hasStatusEffect(effect)
        ? effect.getRemainingDuration()
        : 0;
  }

  /** Portion of an active timed consumable still remaining, from zero to one. */
  public float getRemainingFraction(String id) {
    TimedStatusEffect effect = id == null ? null : active.get(id);
    long remaining = getRemainingMs(id);
    return effect == null || effect.getDuration() <= 0
        ? 0f
        : Math.min(1f, (float) remaining / effect.getDuration());
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
}
