package com.csse3200.game.components;

import com.csse3200.game.services.GameTime;

public class StatusEffectsControllerComponent extends Component {

  private CombatStatsComponent combatStatsComponent;
  private final GameTime time = new GameTime();

  private int burning;
  private long lastBurn;
  private final long BURN_COOLDOWN = 1000;

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
   * Adds stacks to burning.
   *
   * @param stacks the number of stacks of burning to add.
   */
  public void burningOn(int stacks) {
    if (stacks <= 0) {
      throw new IllegalArgumentException("Stacks of burning must be > 0");
    }
    if (burning == 0) {
      lastBurn = time.getTime();
    }
    burning += stacks;
  }

  /**
   * Updates the state of all status effects. Burning: If time since last burn > burn cooldown,
   * deals 1 damage per stack of burn.
   */
  public void update() {
    if (burning > 0 && time.getTimeSince(lastBurn) > BURN_COOLDOWN) {
      combatStatsComponent.takeDamage(burning); // Deals 1 damage per stack of burning.
      lastBurn = time.getTime();
    }
  }
}
