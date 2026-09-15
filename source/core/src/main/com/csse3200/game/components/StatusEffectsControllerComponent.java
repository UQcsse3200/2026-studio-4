package com.csse3200.game.components;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.statuseffects.Damageable;
import com.csse3200.game.components.statuseffects.Shield;
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
  private Shield shield;
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
    entity.getEvents().addListener("shieldAbsorb", this::activateAbsorb);
    entity.getEvents().addListener("shieldTimed", this::activateTimed);
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
      case 's':
        for (int i = 0; i < stacks; i++) {
          addStatusEffect(StatusEffectsFactory.createSlow(combatStatsComponent));
        }
        break;
      case 'S':
        for (int i = 0; i < stacks; i++) {
          addStatusEffect(StatusEffectsFactory.createSpeed(combatStatsComponent));
        }
        break;
      case 'v':
        for (int i = 0; i < stacks; i++) {
          addStatusEffect(StatusEffectsFactory.createVulnerable());
        }
        break;
      case 'f':
        addStatusEffect(StatusEffectsFactory.createFreeze(combatStatsComponent));
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
   * Activates the entity's absorb-mode shield in response to the {@code shieldAbsorb} event.
   *
   * <p>A shield is created and registered lazily the first time either shield mode is activated.
   */
  public void activateAbsorb() {
    ensureShield();
    shield.activateAbsorb();
    triggerShieldUi();
  }

  /**
   * Activates the entity's timed shield in response to the {@code shieldTimed} event.
   *
   * <p>A shield is created and registered lazily the first time either shield mode is activated.
   */
  public void activateTimed() {
    ensureShield();
    shield.activateTimed();
    triggerShieldUi();
  }

  /**
   * Passes incoming damage through the entity's shield, if one has been created.
   *
   * @param damage the incoming damage amount
   * @return the damage remaining after shield mitigation
   */
  public int modifyIncomingDamage(int damage) {
    if (shield == null) {
      return damage;
    }

    int remainingDamage = shield.modifyIncomingDamage(damage);
    triggerShieldUi();
    return remainingDamage;
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
      // Each of those takes the effect off the list, so the membership check covers all three.
      if (statusEffects.contains(effect) && effect.update() && statusEffects.remove(effect)) {
        if (removed == null) {
          removed = new ArrayList<>();
        }
        removed.add(effect);
      }
    }
    if (removed != null) {
      notifyRemoved(removed);
    }
    triggerShieldUi();
  }

  /**
   * Runs the damage effect of all damageable statuseffects.
   *
   * @param damage The damage object to be interacted with.
   */
  public void damage(Damage damage) {
    ArrayList<StatusEffect> removal = new ArrayList<>();

    for (StatusEffect effect : new ArrayList<>(statusEffects)) {
      if (effect instanceof Damageable damageable && damageable.damage(damage)) {
        removal.addLast(effect);
      }
    }

    statusEffects.removeAll(removal);
    notifyRemoved(removal);
  }

  /**
   * Runs the removal callback of every effect that has just come off this entity.
   *
   * <p>Call this only once the effects are already off the list, so anything a callback asks the
   * controller sees the finished state rather than a half-cleared one.
   *
   * @param removed the effects that were taken off, in the order they were removed.
   */
  private static void notifyRemoved(List<StatusEffect> removed) {
    for (StatusEffect effect : removed) {
      effect.onRemoved();
    }
  }

  /**
   * Ends every effect on this entity and stops the controller for good.
   *
   * <p>Each effect still running is removed and told, so none is left believing it is on an entity
   * that no longer exists. A disposed controller ticks nothing further and refuses new effects.
   */
  @Override
  public void dispose() {
    disposed = true;
    clearStatusEffects();
  }

  private void ensureShield() {
    if (shield == null) {
      shield = StatusEffectsFactory.createShield();
      statusEffects.add(shield);
    }
  }

  private void triggerShieldUi() {
    if (entity == null || shield == null) {
      return;
    }
    entity.getEvents().trigger("updateShield", shield.getCurrent(), shield.getMax());
  }
}
