package com.csse3200.game.components;

public class BossPhaseComponent extends Component {
  private int currentPhase = 1;
  private CombatStatsComponent combatStatsComponent;

  @Override
  public void create() {
    super.create();
    combatStatsComponent = entity.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("updateHealth", this::checkPhaseTransition);
  }

  private void checkPhaseTransition(int currentHealth) {
    int maxHealth = combatStatsComponent.getMaxHealth();
    float healthPercentage = (float) currentHealth / maxHealth;

    if (healthPercentage <= 0.5f && currentPhase == 1) {
      currentPhase = 2;
      triggerPhaseTwo();
    }
  }

  private void triggerPhaseTwo() {
    entity.getEvents().trigger("enragePhaseStarted");
  }

  public int getCurrentPhase() {
    return currentPhase;
  }
}
