package com.csse3200.game.components;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the status effects currently on an entity and drives them, treating every one of them as a
 * plain {@link StatusEffect} so that nothing here depends on any concrete effect.
 *
 * <p>Effects are ticked once per frame in {@link #update()} and dropped when they report done. The
 * query methods ({@link #isConcealed()}, {@link #getStatMultiplier(Stat)}, {@link #getTint()})
 * combine the answers of the effects still running. They are pure reads: an effect past its
 * deadline is ignored the instant it expires, but it is only taken off the list, and its removal
 * callback only runs, on the next frame update. That keeps renderers and AI, which query every
 * frame, from triggering lifecycle callbacks mid-query.
 */
public class StatusEffectsControllerComponent extends Component {

  private CombatStatsComponent combatStatsComponent;

  private final ArrayList<StatusEffect> statusEffects = new ArrayList<>();
  private boolean disposed;

  /**
   * Returns whether hostiles should currently be unable to find, chase or damage the entity. An
   * entity without a controller is simply not concealed.
   *
   * <p>This is the one question enemy AI, boss pursuit and contact damage ask. It names no ability,
   * so any effect that reports {@link StatusEffect#concealsOwner()} hides whoever is wearing it.
   *
   * @param entity the entity being looked for; must not be null
   */
  public static boolean isConcealed(Entity entity) {
    StatusEffectsControllerComponent effects =
        entity.getComponent(StatusEffectsControllerComponent.class);
    return effects != null && effects.isConcealed();
  }

  /**
   * Returns the combined sprite tint of the entity's active effects, or null when nothing tints it.
   *
   * @see StatusEffect#getTint()
   */
  public static Color getTint(Entity entity) {
    StatusEffectsControllerComponent effects = findOn(entity);
    return effects == null ? null : effects.getTint();
  }

  private static StatusEffectsControllerComponent findOn(Entity entity) {
    return entity == null ? null : entity.getComponent(StatusEffectsControllerComponent.class);
  }

  /** Returns whether any running effect hides this entity from hostiles. */
  public boolean isConcealed() {
    for (StatusEffect effect : statusEffects) {
      if (!effect.isExpired() && effect.concealsOwner()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns what running effects together multiply the given effective stat by, which is 1 when
   * nothing is running. Effects compose, so two buffs multiply rather than one winning.
   */
  public float getStatMultiplier(Stat stat) {
    float multiplier = 1f;
    for (StatusEffect effect : statusEffects) {
      if (!effect.isExpired()) {
        multiplier *= effect.getStatMultiplier(stat);
      }
    }
    return multiplier;
  }

  /**
   * Returns the combined tint of running effects, or null when none tints. Tints multiply, so being
   * hidden and buffed at once shows both.
   */
  public Color getTint() {
    Color combined = null;
    for (StatusEffect effect : statusEffects) {
      Color tint = effect.isExpired() ? null : effect.getTint();
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

  /** Returns whether the effect is on this entity and still running. */
  public boolean hasStatusEffect(StatusEffect effect) {
    return effect != null && statusEffects.contains(effect) && !effect.isExpired();
  }

  public boolean isDisposed() {
    return disposed;
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
    entity.getEvents().addListener("entityDied", this::clearStatusEffects);
  }

  /**
   * Puts a status effect on this entity. The controller now owns its lifetime: it is ticked each
   * frame and its {@link StatusEffect#onRemoved()} runs once when it comes off.
   *
   * @throws IllegalStateException if the controller is disposed or already holds this effect.
   */
  public void addStatusEffect(StatusEffect effect) {
    if (effect == null) {
      throw new IllegalArgumentException("Status effect must not be null");
    }
    if (disposed || statusEffects.contains(effect)) {
      throw new IllegalStateException("Effect already applied or controller disposed");
    }
    statusEffects.addLast(effect);
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
          addStatusEffect(StatusEffectsFactory.createBurn(combatStatsComponent));
        }
        break;
      case 'r':
        for (int i = 0; i < stacks; i++) {
          addStatusEffect(StatusEffectsFactory.createRegeneration(combatStatsComponent));
        }
        break;
      default:
        throw new IllegalArgumentException(
            "statusEffect must be a valid character representation of a status effect.");
    }
  }

  /** Takes an effect off early. Removing one that is not on this entity changes nothing. */
  public void removeStatusEffect(StatusEffect effect) {
    if (effect != null && statusEffects.remove(effect)) {
      effect.onRemoved();
    }
  }

  /** Takes every effect off at once, running each removal callback after the list is empty. */
  public void clearStatusEffects() {
    if (statusEffects.isEmpty()) {
      return;
    }
    List<StatusEffect> removed = new ArrayList<>(statusEffects);
    statusEffects.clear();
    notifyRemoved(removed);
  }

  /**
   * Updates the state of all status effects. Removes status effects that return true from update,
   * then tells each removed effect, so no callback ever sees a half-cleared list.
   */
  @Override
  public void update() {
    if (disposed) {
      return;
    }
    // Queries and callbacks run every frame, so allocate only when something actually ended.
    List<StatusEffect> removed = null;
    for (StatusEffect effect : new ArrayList<>(statusEffects)) {
      // A tick may dispose this controller, clear the list (a burn that kills) or remove this
      // effect, in which case the effect has already been told; only a live removal is notified.
      if (!disposed
          && statusEffects.contains(effect)
          && effect.update()
          && statusEffects.remove(effect)) {
        if (removed == null) {
          removed = new ArrayList<>();
        }
        removed.add(effect);
      }
    }
    if (removed != null) {
      notifyRemoved(removed);
    }
  }

  private static void notifyRemoved(List<StatusEffect> removed) {
    for (StatusEffect effect : removed) {
      effect.onRemoved();
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    clearStatusEffects();
  }
}
