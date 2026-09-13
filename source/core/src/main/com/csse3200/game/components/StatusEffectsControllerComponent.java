package com.csse3200.game.components;
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
   * Adds stacks to burning. Throws IllegalArgumentException if stacks <= 0.
   *
   * @param stacks the number of stacks of burning to add.
   */
  public void burningOn(int stacks) {
    if (stacks <= 0) {
      throw new IllegalArgumentException("Stacks of burning must be > 0");
    }
    for (int i = 0; i < stacks; i++) {
      statusEffects.addLast(StatusEffectsFactory.CreateBurn(combatStatsComponent));
    }
  }

  public void activateAbsorb() {
    ensureShield();
    shield.activateAbsorb();
    triggerShieldUi();
}

public void activateTimed() {
  ensureShield();
  shield.activateTimed();
  triggerShieldUi();
}
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