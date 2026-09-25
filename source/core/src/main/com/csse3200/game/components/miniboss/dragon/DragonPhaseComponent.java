package com.csse3200.game.components.miniboss.dragon;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;

/** Controls the dragon's permanent transition to phase two at 50% health. */
public class DragonPhaseComponent extends Component {
  public static final String ENRAGE_STARTED = "dragonEnrageStarted";

  private CombatStatsComponent combatStatsComponent;
  private int currentPhase = 1;

  @Override
  public void create() {
    combatStatsComponent = entity.getComponent(CombatStatsComponent.class);
    if (combatStatsComponent == null) {
      throw new IllegalStateException("DragonPhaseComponent requires CombatStatsComponent");
    }

    entity.getEvents().addListener("updateHealth", (Integer health) -> checkPhase());
    entity.getEvents().addListener("updateMaxHealth", (Integer maximum) -> checkPhase());
    checkPhase();
  }

  private void checkPhase() {
    if (currentPhase == 2) {
      return;
    }

    int health = combatStatsComponent.getHealth();
    int maxHealth = combatStatsComponent.getMaxHealth();
    if (health <= 0 || maxHealth <= 0 || (long) health * 2 > maxHealth) {
      return;
    }

    currentPhase = 2;
    entity.getEvents().trigger(ENRAGE_STARTED);
  }

  /** Returns the current combat phase, either 1 or 2. */
  public int getCurrentPhase() {
    return currentPhase;
  }

  /** Returns whether the dragon has entered phase two. */
  public boolean isEnraged() {
    return currentPhase == 2;
  }
}
