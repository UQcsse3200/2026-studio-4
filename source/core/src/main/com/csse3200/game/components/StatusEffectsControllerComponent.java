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
  @Override
  public void create() {
    combatStatsComponent = entity.getComponent(CombatStatsComponent.class);
    if (combatStatsComponent == null) {
      throw new IllegalStateException(
          "StatusEffectsController requires CombatStatsComponent on the same entity.");
    }
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
