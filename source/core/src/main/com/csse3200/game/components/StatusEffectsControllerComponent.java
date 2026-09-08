package com.csse3200.game.components;

import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import java.util.ArrayList;

public class StatusEffectsControllerComponent extends Component {

  private CombatStatsComponent combatStatsComponent;

  private final ArrayList<StatusEffect> statusEffects = new ArrayList<>();

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
  }

  /**
   * Adds stacks to burning. Throws IllegalArgumentException if stacks <= 0.
   *
   * @param stacks the number of stacks of burning to add.
   */
  public void addStatusEffect(int stacks, char statusEffect) {
    if (stacks <= 0) {
      throw new IllegalArgumentException("Stacks must be > 0");
    }
    switch (statusEffect) {
      case 'b':
        for (int i = 0; i < stacks; i++) {
          statusEffects.addLast(StatusEffectsFactory.CreateBurn(combatStatsComponent));
        }
      case 'r':
        for (int i = 0; i < stacks; i++) {
          statusEffects.addLast(StatusEffectsFactory.CreateRegeneration(combatStatsComponent));
        }
    }
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
  }
}
