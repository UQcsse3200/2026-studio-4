package com.csse3200.game.components;

import com.csse3200.game.components.statuseffects.Damageable;
import com.csse3200.game.components.statuseffects.Shield;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import java.util.ArrayList;

public class StatusEffectsControllerComponent extends Component {

  private CombatStatsComponent combatStatsComponent;

  private final ArrayList<StatusEffect> statusEffects = new ArrayList<>();
  private Shield shield;

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
      case 's':
        for (int i = 0; i < stacks; i++) {
          statusEffects.addLast(StatusEffectsFactory.createSlow(combatStatsComponent));
        }
        break;
      case 'S':
        for (int i = 0; i < stacks; i++) {
          statusEffects.addLast(StatusEffectsFactory.createSpeed(combatStatsComponent));
        }
        break;
      case 'v':
        for (int i = 0; i < stacks; i++) {
          statusEffects.addLast(StatusEffectsFactory.createVulnerable());
        }
        break;
      case 'f':
        statusEffects.addLast(StatusEffectsFactory.createFreeze(combatStatsComponent));
        break;
      default:
        throw new IllegalArgumentException(
            "statusEffect must be a valid character representation of a status effect.");
    }
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
   * Updates the state of all status effects. Removes status effects that return true from update.
   */
  @Override
  public void update() {
    ArrayList<StatusEffect> removal = new ArrayList<>();
    for (StatusEffect effect : statusEffects) {
      if (effect.update()) {
        removal.addLast(effect);
      }
    }
    for (StatusEffect effect : removal) {
      statusEffects.remove(effect);
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

    for (StatusEffect effect : statusEffects) {
      if (effect instanceof Damageable damageable && damageable.damage(damage)) {
        removal.addLast(effect);
      }
    }

    for (StatusEffect effect : removal) {
      statusEffects.remove(effect);
    }
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
