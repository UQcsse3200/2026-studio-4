package com.csse3200.game.components;

import com.csse3200.game.services.GameTime;

public class StatusEffectsControllerComponent extends Component {

  private CombatStatsComponent combatStatsComponent;
  private final GameTime time = new GameTime();

  private int burning;
  private long lastBurn;
  private final long BURN_COOLDOWN = 1000;

  public void create() {
    combatStatsComponent = entity.getComponent(CombatStatsComponent.class);
    if (combatStatsComponent == null) {
      throw new IllegalStateException(
          "StatusEffectsController requires CombatStatsComponent on the same entity.");
    }
  }

  public void burningOn(int stacks) {
    if (burning == 0) {
      lastBurn = time.getTime();
    }
    burning += stacks;
  }

  public void update() {
    if (burning > 0 && time.getTimeSince(lastBurn) > BURN_COOLDOWN) {
      combatStatsComponent.takeDamage(burning); // Deals 1 damage per stack of burning.
      lastBurn = time.getTime();
    }
  }
}
