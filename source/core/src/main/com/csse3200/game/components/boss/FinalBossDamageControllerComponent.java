package com.csse3200.game.components.boss;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;

/** Controls the Final Boss shield, damage reduction and scripted health floors. */
public class FinalBossDamageControllerComponent extends Component {
  private CombatStatsComponent stats;
  private boolean shielded;

  @Override
  public void create() {
    requireStats();
    entity.getEvents().addListener("damageBlocked", this::onDamageBlocked);
    enableShield();
  }

  /** Enables the shield and blocks all incoming damage. */
  public void enableShield() {
    CombatStatsComponent combatStats = requireStats();
    combatStats.setMinimumHealth(0);
    combatStats.setIncomingDamageMultiplier(1f);
    combatStats.setInvulnerable(true);

    if (!shielded) {
      shielded = true;
      entity.getEvents().trigger(FinalBossEvents.SHIELD_ACTIVATED);
    }
  }

  /**
   * Opens a vulnerability window with reduced incoming damage and a health floor.
   *
   * @param damageMultiplier multiplier applied to incoming damage
   * @param minimumHealth lowest health reachable through incoming attacks
   */
  public void openVulnerabilityWindow(float damageMultiplier, int minimumHealth) {
    CombatStatsComponent combatStats = requireStats();
    combatStats.setInvulnerable(false);
    combatStats.setIncomingDamageMultiplier(damageMultiplier);
    combatStats.setMinimumHealth(minimumHealth);

    if (shielded) {
      shielded = false;
      entity.getEvents().trigger(FinalBossEvents.SHIELD_DEACTIVATED);
    }
  }

  /** Removes all Stage 1 damage restrictions before Stage 2 begins. */
  public void disableStageOneProtection() {
    CombatStatsComponent combatStats = requireStats();
    combatStats.setInvulnerable(false);
    combatStats.setIncomingDamageMultiplier(1f);
    combatStats.setMinimumHealth(0);

    if (shielded) {
      shielded = false;
      entity.getEvents().trigger(FinalBossEvents.SHIELD_DEACTIVATED);
    }
  }

  /** Returns whether the shield is currently active. */
  public boolean isShielded() {
    return shielded;
  }

  private void onDamageBlocked() {
    if (shielded) {
      entity.getEvents().trigger(FinalBossEvents.SHIELD_HIT);
    }
  }

  private CombatStatsComponent requireStats() {
    if (stats == null) {
      stats = entity.getComponent(CombatStatsComponent.class);
    }

    if (stats == null) {
      throw new IllegalStateException(
          "FinalBossDamageControllerComponent requires CombatStatsComponent");
    }

    return stats;
  }
}
