package com.csse3200.game.components.boss;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Controls the health threshold transition from Stage 2 to Stage 3, and manages attack/pause cycle.
 */
public class FinalBossStageTwoComponent extends Component {
  private final FinalBossStageTwoConfig stageTwoConfig;

  private FinalBossPhaseControllerComponent phaseController;
  private CombatStatsComponent bossStats;
  private FinalBossMovementComponent movementComponent;
  private boolean transitionSent;

  // Attack/pause cycle tracking
  private boolean isAttacking = true;
  private float cycleTimer = 0f;

  public FinalBossStageTwoComponent(FinalBossStageTwoConfig stageTwoConfig) {
    if (stageTwoConfig == null) {
      throw new IllegalArgumentException("Configs must not be null");
    }

    stageTwoConfig.validate();
    this.stageTwoConfig = stageTwoConfig;
  }

  @Override
  public void create() {
    phaseController = entity.getComponent(FinalBossPhaseControllerComponent.class);
    bossStats = entity.getComponent(CombatStatsComponent.class);
    movementComponent = entity.getComponent(FinalBossMovementComponent.class);

    if (phaseController == null || bossStats == null || movementComponent == null) {
      throw new IllegalStateException(
          "FinalBossStageTwoComponent requires FinalBossPhaseControllerComponent, "
              + "CombatStatsComponent, and FinalBossMovementComponent");
    }

    entity.getEvents().addListener("updateHealth", this::onBossHealthChanged);
  }

  @Override
  public void update() {
    if (phaseController.getCurrentPhase() != FinalBossPhase.STAGE_TWO) {
      return;
    }

    GameTime time = ServiceLocator.getTimeSource();
    if (time == null) {
      return;
    }

    float deltaTime = time.getDeltaTime();
    if (!Float.isFinite(deltaTime) || deltaTime <= 0f) {
      return;
    }

    cycleTimer += deltaTime;

    if (isAttacking) {
      if (cycleTimer >= stageTwoConfig.attackDuration) {
        // Switch to pause phase
        isAttacking = false;
        cycleTimer = 0f;
        movementComponent.setMode(FinalBossMovementComponent.Mode.STOPPED);
      }
    } else {
      if (cycleTimer >= stageTwoConfig.pauseDuration) {
        // Switch back to attacking phase
        isAttacking = true;
        cycleTimer = 0f;
        movementComponent.setMode(FinalBossMovementComponent.Mode.STEP_TOWARDS_PLAYER);
      }
    }
  }

  /** Returns whether the boss is currently in the attacking phase. */
  public boolean isAttacking() {
    return isAttacking;
  }

  /** Triggers Stage 3 transition when health falls below the configured threshold. */
  private void onBossHealthChanged(Integer health) {
    if (transitionSent
        || health <= 0
        || phaseController.getCurrentPhase() != FinalBossPhase.STAGE_TWO) {
      return;
    }

    int healthThreshold =
        Math.round(bossStats.getMaxHealth() * stageTwoConfig.stageThreeHealthThreshold);

    if (health <= healthThreshold) {
      transitionSent = true;
      entity.getEvents().trigger(FinalBossEvents.STAGE_COMPLETED, FinalBossPhase.STAGE_TWO);
    }
  }
}
