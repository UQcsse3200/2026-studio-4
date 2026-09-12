package com.csse3200.game.components;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.components.statuseffects.TimedEffect;
import com.csse3200.game.entities.Entity;
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

  /**
   * Returns whether hostiles should ignore the entity: either there is no entity, or something on
   * it is currently concealing it. Callers that pass this check may dereference the entity.
   *
   * <p>This is the question enemy AI, boss pursuit and contact damage ask. It names no ability, so
   * any effect that reports {@link TimedEffect#concealsOwner()} hides whoever is wearing it.
   */
  public static boolean isUntargetable(Entity entity) {
    StatusEffectsControllerComponent effects = findOn(entity);
    return entity == null || (effects != null && effects.isConcealed());
  }

  /**
   * Returns the combined sprite tint of the entity's active effects, or null when nothing tints it.
   *
   * @see TimedEffect#getTint()
   */
  public static Color getTint(Entity entity) {
    StatusEffectsControllerComponent effects = findOn(entity);
    return effects == null ? null : effects.getTint();
  }

  private static StatusEffectsControllerComponent findOn(Entity entity) {
    return entity == null ? null : entity.getComponent(StatusEffectsControllerComponent.class);
  }

  /** Returns whether any active effect hides this entity from hostiles. */
  public boolean isConcealed() {
    refreshTimedEffects();
    for (TimedEffect effect : timedEffects) {
      if (effect.isActive() && effect.concealsOwner()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns what active effects together multiply this entity's effective combat stats by, which is
   * 1 when nothing is running. Effects compose, so two buffs multiply rather than one winning.
   */
  public float getStatMultiplier() {
    refreshTimedEffects();
    float multiplier = 1f;
    for (TimedEffect effect : timedEffects) {
      if (effect.isActive()) {
        multiplier *= effect.getStatMultiplier();
      }
    }
    return multiplier;
  }

  /**
   * Returns the combined tint of active effects, or null when none tints. Tints multiply, so being
   * hidden and buffed at once shows both.
   */
  public Color getTint() {
    refreshTimedEffects();
    Color combined = null;
    for (TimedEffect effect : timedEffects) {
      Color tint = effect.isActive() ? effect.getTint() : null;
      if (tint == null) {
        continue;
      }
      if (combined == null) {
        combined = new Color(tint);
      } else {
        combined.mul(tint);
      }
    }
    return combined;
  }

  public void refreshTimedEffects() {
    clearTimedEffects(disposed || (combatStatsComponent != null && combatStatsComponent.isDead()));
  }

  public void clearTimedEffects() {
    clearTimedEffects(true);
  }

  private void clearTimedEffects(boolean all) {
    // Queries run this before answering, so allocate only when something actually ended.
    ArrayList<TimedEffect> ended = null;
    for (TimedEffect effect : timedEffects) {
      if (effect.isActive() && (all || effect.update())) {
        effect.clear();
        if (ended == null) {
          ended = new ArrayList<>();
        }
        ended.add(effect);
      }
    }
    if (ended == null) {
      return;
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
