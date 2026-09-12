package com.csse3200.game.components;

import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class StatusEffectsControllerComponent extends Component {

  private CombatStatsComponent combatStatsComponent;

  private final ArrayList<StatusEffect> statusEffects = new ArrayList<>();
  private final LinkedHashMap<Class<? extends TimedStatusEffect>, TimedStatusEffect> timedEffects =
      new LinkedHashMap<>();
  private boolean disposed;

  /** Registers one reusable effect per concrete type, in lifecycle notification order. */
  public void registerEffect(TimedStatusEffect effect) {
    if (disposed || timedEffects.containsKey(effect.getClass())) {
      throw new IllegalStateException("Effect already registered or controller disposed");
    }
    timedEffects.put(effect.getClass(), effect);
  }

  /** Queries deadlines only; never ticks burning or regeneration. */
  public <T extends TimedStatusEffect> T getEffect(Class<T> type) {
    refreshTimedEffects();
    return type.cast(timedEffects.get(type));
  }

  /** Removes the active state, retaining the registration for the next activation. */
  public void removeEffect(Class<? extends TimedStatusEffect> type) {
    refreshTimedEffects();
    TimedStatusEffect effect = timedEffects.get(type);
    if (effect != null && effect.isActive()) {
      effect.clear();
      effect.notifyEnded();
    }
  }

  public boolean isDisposed() {
    return disposed;
  }

  public void refreshTimedEffects() {
    clearTimedEffects(disposed || (combatStatsComponent != null && combatStatsComponent.isDead()));
  }

  public void clearTimedEffects() {
    clearTimedEffects(true);
  }

  private void clearTimedEffects(boolean all) {
    ArrayList<TimedStatusEffect> ended = new ArrayList<>();
    for (TimedStatusEffect effect : timedEffects.values()) {
      if (effect.isActive() && (all || effect.update())) {
        effect.clear();
        ended.add(effect);
      }
    }
    // Clear every expired state before any callback can reenter or activate another effect.
    for (TimedStatusEffect effect : ended) {
      effect.notifyEnded();
    }
  }

  /**
   * Caches the CombatStatsComponent from entity.
   *
   * <p>Throws IllegalStateException if CombatStatsComponent is null.
   */
  @Override
  public void create() {
    combatStatsComponent = entity.getComponent(CombatStatsComponent.class);
    if (combatStatsComponent == null) {
      throw new IllegalStateException(
          "StatusEffectsController requires CombatStatsComponent on the same entity.");
    }
    entity.getEvents().addListener("entityDied", this::refreshTimedEffects);
  }

  /**
   * Adds stacks to a status effect.
   *
   * @param statusEffect the status effect to add stacks of. A single character indicator for each
   *     effect. 'b' for burning. 'r' for regeneration.
   * @param stacks the number of stacks to add.
   */
  public void addStatusEffect(int stacks, char statusEffect) {
    if (stacks <= 0) {
      throw new IllegalArgumentException("Stacks must be > 0");
    }
    switch (statusEffect) {
      case 'b':
        for (int i = 0; i < stacks; i++) {
          statusEffects.addLast(StatusEffectsFactory.createBurn(combatStatsComponent));
        }
        break;
      case 'r':
        for (int i = 0; i < stacks; i++) {
          statusEffects.addLast(StatusEffectsFactory.createRegeneration(combatStatsComponent));
        }
        break;
      default:
        throw new IllegalArgumentException(
            "statusEffect must be a valid character representation of a status effect.");
    }
  }

  /**
   * Updates the state of all status effects. Removes status effects that return true from update.
   */
  @Override
  public void update() {
    if (disposed) {
      return;
    }
    refreshTimedEffects();
    ArrayList<StatusEffect> removal = new ArrayList<>();
    for (StatusEffect effect : new ArrayList<>(statusEffects)) {
      if (disposed) {
        break;
      }
      if (effect.update()) {
        removal.addLast(effect);
      }
    }
    for (StatusEffect effect : removal) {
      statusEffects.remove(effect);
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    clearTimedEffects();
    timedEffects.clear();
    statusEffects.clear();
  }
}
