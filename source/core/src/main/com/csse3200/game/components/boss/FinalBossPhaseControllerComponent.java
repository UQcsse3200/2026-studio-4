package com.csse3200.game.components.boss;

import com.csse3200.game.components.Component;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/** Controls sequential transitions between the three Final Boss stages. */
public class FinalBossPhaseControllerComponent extends Component {
  private static final float STAGE_TRANSITION_DURATION = 3f;

  private FinalBossPhase currentPhase = FinalBossPhase.STAGE_ONE;
  private float transitionRemaining;

  @Override
  public void create() {
    entity.getEvents().addListener(FinalBossEvents.STAGE_COMPLETED, this::completeStage);
  }

  @Override
  public void update() {
    if (transitionRemaining <= 0f) {
      return;
    }

    GameTime time = ServiceLocator.getTimeSource();
    float deltaTime = time == null ? 0f : time.getDeltaTime();
    if (!Float.isFinite(deltaTime) || deltaTime <= 0f) {
      return;
    }

    transitionRemaining = Math.max(0f, transitionRemaining - deltaTime);

    if (transitionRemaining <= 0f) {
      FinalBossStageThreeComponent stageThree =
          entity.getComponent(FinalBossStageThreeComponent.class);
      if (currentPhase == FinalBossPhase.STAGE_THREE && stageThree != null) {
        stageThree.startWaveOne();
        return;
      }
      FinalBossDamageControllerComponent protection =
          entity.getComponent(FinalBossDamageControllerComponent.class);
      if (protection != null) {
        protection.disableStageOneProtection();
      }

      if (currentPhase == FinalBossPhase.STAGE_TWO) {
        FinalBossStageTwoComponent stageTwo = entity.getComponent(FinalBossStageTwoComponent.class);
        if (stageTwo != null) stageTwo.applyStageThreeHealthFloor();
        FinalBossMovementComponent movement = entity.getComponent(FinalBossMovementComponent.class);
        if (movement != null) {
          movement.enableChargeAttacks();
        }
      }
    }
  }

  public FinalBossPhase getCurrentPhase() {
    return currentPhase;
  }

  /** Returns whether the boss is in the brief invulnerable window right after a stage change. */
  public boolean isTransitioning() {
    return transitionRemaining > 0f;
  }

  public boolean completeStage(FinalBossPhase completedPhase) {
    if (completedPhase != currentPhase || currentPhase == FinalBossPhase.DEFEATED) {
      return false;
    }

    currentPhase = nextPhase(currentPhase);
    if (currentPhase == FinalBossPhase.DEFEATED) transitionRemaining = 0f;
    if (currentPhase == FinalBossPhase.STAGE_TWO) {
      // Charge attacks are enabled once the transition invulnerability window ends.
      FinalBossMovementComponent movement = entity.getComponent(FinalBossMovementComponent.class);
      if (movement != null) {
        movement.setMode(FinalBossMovementComponent.Mode.STEP_TOWARDS_PLAYER);
      }
    } else if (currentPhase == FinalBossPhase.STAGE_THREE) {
      FinalBossMovementComponent movement = entity.getComponent(FinalBossMovementComponent.class);
      if (movement != null) {
        movement.cancelChargeAttack();
        movement.setMode(FinalBossMovementComponent.Mode.STOPPED);
      }
    }

    if (currentPhase != FinalBossPhase.DEFEATED) {
      transitionRemaining = STAGE_TRANSITION_DURATION;
      FinalBossDamageControllerComponent protection =
          entity.getComponent(FinalBossDamageControllerComponent.class);
      if (protection != null) {
        protection.enableShield();
      }
    }

    entity.getEvents().trigger(FinalBossEvents.PHASE_CHANGED, currentPhase);
    return true;
  }

  private static FinalBossPhase nextPhase(FinalBossPhase phase) {
    switch (phase) {
      case STAGE_ONE:
        return FinalBossPhase.STAGE_TWO;
      case STAGE_TWO:
        return FinalBossPhase.STAGE_THREE;
      case STAGE_THREE:
      default:
        return FinalBossPhase.DEFEATED;
    }
  }
}
