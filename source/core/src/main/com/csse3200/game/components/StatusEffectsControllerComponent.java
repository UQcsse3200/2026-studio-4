package com.csse3200.game.components;

import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.components.statuseffects.TimedEffect;
import java.util.ArrayList;

public class StatusEffectsControllerComponent extends Component {

  private CombatStatsComponent combatStatsComponent;

  private final ArrayList<StatusEffect> statusEffects = new ArrayList<>();
  private final ArrayList<TimedEffect> timedEffects = new ArrayList<>();
  private boolean disposed;

  /**
   * Takes ownership of expiring a timed effect, in lifecycle notification order. The caller keeps
   * the reference and decides when to activate it.
   */
  public void registerEffect(TimedEffect effect) {
    if (disposed || timedEffects.contains(effect)) {
      throw new IllegalStateException("Effect already registered or controller disposed");
    }
    timedEffects.add(effect);
  }

  /** Stops an active effect, keeping the registration for the next activation. */
  public void removeEffect(TimedEffect effect) {
    refreshTimedEffects();
    if (effect != null && timedEffects.contains(effect) && effect.isActive()) {
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
    ArrayList<TimedEffect> ended = new ArrayList<>();
    for (TimedEffect effect : timedEffects) {
      if (effect.isActive() && (all || effect.update())) {
        effect.clear();
        ended.add(effect);
      }
    }
    // Clear every expired state before any callback can reenter or activate another effect.
    for (TimedEffect effect : ended) {
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
