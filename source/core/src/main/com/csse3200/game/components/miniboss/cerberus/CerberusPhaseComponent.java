package com.csse3200.game.components.miniboss.cerberus;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;

/** Enters phase two when the combined health of all heads reaches 50%. */
public class CerberusPhaseComponent extends Component {
  private static final String UPDATE_HEALTH = "updateHealth";
  private static final String UPDATE_MAX_HEALTH = "updateMaxHealth";
  private static final String ENRAGE_STARTED = "enragePhaseStarted";

  private final Entity leftHead;
  private final Entity rightHead;

  private CombatStatsComponent middleStats;
  private CombatStatsComponent leftStats;
  private CombatStatsComponent rightStats;
  private int currentPhase = 1;

  public CerberusPhaseComponent(Entity leftHead, Entity rightHead) {
    this.leftHead = leftHead;
    this.rightHead = rightHead;
  }

  @Override
  public void create() {
    middleStats = entity.getComponent(CombatStatsComponent.class);
    leftStats = leftHead.getComponent(CombatStatsComponent.class);
    rightStats = rightHead.getComponent(CombatStatsComponent.class);

    listenTo(entity);
    listenTo(leftHead);
    listenTo(rightHead);
    checkPhase();
  }

  private void listenTo(Entity head) {
    head.getEvents().addListener(UPDATE_HEALTH, (Integer health) -> checkPhase());
    head.getEvents().addListener(UPDATE_MAX_HEALTH, (Integer health) -> checkPhase());
  }

  private void checkPhase() {
    if (currentPhase == 2) {
      return;
    }
    long health = (long) middleStats.getHealth() + leftStats.getHealth() + rightStats.getHealth();
    long maximum =
        (long) middleStats.getMaxHealth() + leftStats.getMaxHealth() + rightStats.getMaxHealth();
    if (maximum <= 0 || health <= 0 || health * 2 > maximum) {
      return;
    }
    currentPhase = 2;
    notifyLivingHead(entity, middleStats);
    notifyLivingHead(leftHead, leftStats);
    notifyLivingHead(rightHead, rightStats);
  }

  private void notifyLivingHead(Entity head, CombatStatsComponent stats) {
    if (!stats.isDead()) {
      head.getEvents().trigger(ENRAGE_STARTED);
    }
  }

  public int getCurrentPhase() {
    return currentPhase;
  }
}
