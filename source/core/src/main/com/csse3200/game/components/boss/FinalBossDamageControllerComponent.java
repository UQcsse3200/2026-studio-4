package com.csse3200.game.components.boss;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;

/** Controls the Final Boss shield, damage reduction and scripted health floors. */
public class FinalBossDamageControllerComponent extends Component {
  private CombatStatsComponent stats;
  private boolean shielded;
  private final Entity iceDamageSource = new Entity();
  private boolean stageTwoIceProtection;
  private boolean iceDamageInProgress;
  private boolean iceDamagePending;

  @Override
  public void create() {
    requireStats();
    entity.getEvents().addListener("damageBlocked", this::onDamageBlocked);
    entity.getEvents().addListener("damageAttempted", this::onDamageAttempted);
    enableShield();
  }

  /** Enables the shield and blocks all incoming damage. */
  public void enableShield() {
    stageTwoIceProtection = false;
    iceDamagePending = false;
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
    stageTwoIceProtection = false;
    iceDamagePending = false;
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
    stageTwoIceProtection = false;
    iceDamagePending = false;
    CombatStatsComponent combatStats = requireStats();
    combatStats.setInvulnerable(false);
    combatStats.setIncomingDamageMultiplier(1f);
    combatStats.setMinimumHealth(0);

    if (shielded) {
      shielded = false;
      entity.getEvents().trigger(FinalBossEvents.SHIELD_DEACTIVATED);
    }
  }

  /**
   * Keeps the Stage 2 shield active while allowing only this encounter's ice shots to damage it.
   */
  public void enableStageTwoIceProtection(int minimumHealth) {
    if (!isActiveStageTwo()) {
      return;
    }
    CombatStatsComponent combatStats = requireStats();
    combatStats.setMinimumHealth(minimumHealth);
    combatStats.setIncomingDamageMultiplier(1f);
    combatStats.setInvulnerable(true);
    stageTwoIceProtection = true;
    iceDamagePending = false;
    if (!shielded) {
      shielded = true;
      entity.getEvents().trigger(FinalBossEvents.SHIELD_ACTIVATED);
    }
  }

  /**
   * Applies one authorised ice impact through the normal damage and health-floor pipeline.
   *
   * <p>The private source is neither registered nor given a physical body. Its position preserves
   * the normal hit reaction, while a one-use authorisation prevents another attack from sharing the
   * brief damage window, including attacks made by damage callbacks.
   *
   * @param damage raw ice impact damage
   * @param player player whose ice projectile landed
   */
  public void takeStageTwoIceDamage(int damage, Entity player) {
    if (damage <= 0
        || player == null
        || iceDamageInProgress
        || !stageTwoIceProtection
        || !isActiveStageTwo()) {
      return;
    }
    iceDamageSource.setPosition(player.getPosition());
    iceDamageSource.setScale(player.getScale());
    iceDamageInProgress = true;
    iceDamagePending = true;
    try {
      requireStats().takeDamage(damage, iceDamageSource);
    } finally {
      iceDamagePending = false;
      iceDamageInProgress = false;
      // A health-floor callback can already have installed Stage 3's protection.
      if (stageTwoIceProtection) {
        requireStats().setInvulnerable(true);
      }
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

  private void onDamageAttempted(int damage, Entity source) {
    if (!stageTwoIceProtection) {
      return;
    }
    boolean authorised =
        damage > 0 && iceDamagePending && source == iceDamageSource && isActiveStageTwo();
    iceDamagePending = false;
    requireStats().setInvulnerable(!authorised);
  }

  private boolean isActiveStageTwo() {
    FinalBossPhaseControllerComponent phases =
        entity.getComponent(FinalBossPhaseControllerComponent.class);
    return phases != null
        && phases.getCurrentPhase() == FinalBossPhase.STAGE_TWO
        && !phases.isTransitioning()
        && !requireStats().isDead();
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
