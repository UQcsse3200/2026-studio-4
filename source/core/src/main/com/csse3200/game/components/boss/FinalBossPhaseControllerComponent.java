package com.csse3200.game.components.boss;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/** Controls sequential transitions between the three Final Boss stages. */
public class FinalBossPhaseControllerComponent extends Component {
  private static final float STAGE_TRANSITION_DURATION = 3f;

  private FinalBossPhase currentPhase = FinalBossPhase.STAGE_ONE;
  private float transitionRemaining;
  private boolean debugStageStarted;
  private boolean disposed;

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
      finishTransition();
    }
  }

  private void finishTransition() {
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
      startStageTwo();
    }
  }

  private void startStageTwo() {
    FinalBossStageTwoComponent stageTwo = entity.getComponent(FinalBossStageTwoComponent.class);
    if (stageTwo != null) {
      stageTwo.startEncounter();
    }
  }

  public FinalBossPhase getCurrentPhase() {
    return currentPhase;
  }

  /**
   * Boots a fresh boss directly into the requested playable stage for local testing.
   *
   * <p>The room must supply its focused camera before calling this method. This is a one-time entry
   * choice, not a way to reset an ongoing fight or report skipped stages as completed.
   *
   * @return whether the fresh encounter accepted the requested entry stage
   */
  public boolean startDebugStage(FinalBossPhase phase) {
    if (disposed
        || debugStageStarted
        || phase == null
        || phase == FinalBossPhase.DEFEATED
        || currentPhase != FinalBossPhase.STAGE_ONE
        || entity == null) {
      return false;
    }
    FinalBossStageOneComponent stageOne = entity.getComponent(FinalBossStageOneComponent.class);
    FinalBossStageTwoComponent stageTwo = entity.getComponent(FinalBossStageTwoComponent.class);
    FinalBossStageThreeComponent stageThree =
        entity.getComponent(FinalBossStageThreeComponent.class);
    FinalBossMovementComponent movement = entity.getComponent(FinalBossMovementComponent.class);
    FinalBossDamageControllerComponent protection =
        entity.getComponent(FinalBossDamageControllerComponent.class);
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    if (stageOne == null
        || !stageOne.isFreshIntro()
        || movement == null
        || protection == null
        || stats == null
        || stats.isDead()
        || (phase != FinalBossPhase.STAGE_ONE && stageTwo == null)
        || (phase == FinalBossPhase.STAGE_THREE && stageThree == null)) {
      return false;
    }
    int startingHealth =
        switch (phase) {
          case STAGE_TWO -> stageOne.getStageTwoStartingHealth();
          case STAGE_THREE -> stageTwo.getStageThreeStartingHealth();
          default -> stats.getHealth();
        };
    if (startingHealth <= 0) return false;

    debugStageStarted = true;
    movement.prepareDebugStage();
    if (phase == FinalBossPhase.STAGE_ONE) return true;

    // Set entry health while still in INTRO: Stage 2 must not mistake the Stage 3 threshold
    // for a completed fight and award its no-fire-hit achievement during this debug jump.
    stats.setHealth(startingHealth);
    stageOne.dispose();
    transitionRemaining = 0f;
    currentPhase = phase;
    protection.enableShield();
    entity.getEvents().trigger(FinalBossEvents.PHASE_CHANGED, phase);
    finishDebugStageEntry(phase);
    return true;
  }

  private void finishDebugStageEntry(FinalBossPhase expectedPhase) {
    // Phase listeners run synchronously and may dispose the controller or advance the encounter.
    if (disposed || currentPhase != expectedPhase) return;
    FinalBossStageTwoArenaComponent arena =
        entity.getComponent(FinalBossStageTwoArenaComponent.class);
    if (expectedPhase == FinalBossPhase.STAGE_TWO && arena != null) arena.update();
    finishTransition();
  }

  @Override
  public void dispose() {
    disposed = true;
    transitionRemaining = 0f;
  }

  /** Returns whether the boss is in the brief invulnerable window right after a stage change. */
  public boolean isTransitioning() {
    return transitionRemaining > 0f;
  }

  /** Progress of the shared transformation, from zero on entry to one when complete. */
  public float getTransitionProgress() {
    return 1f - transitionRemaining / STAGE_TRANSITION_DURATION;
  }

  public boolean completeStage(FinalBossPhase completedPhase) {
    if (completedPhase != currentPhase || currentPhase == FinalBossPhase.DEFEATED) {
      return false;
    }

    currentPhase = nextPhase(currentPhase);
    if (currentPhase == FinalBossPhase.DEFEATED) transitionRemaining = 0f;
    if (currentPhase == FinalBossPhase.STAGE_TWO) {
      // Stage 2 owns slow movement after the transition; never restart the old charges.
      FinalBossMovementComponent movement = entity.getComponent(FinalBossMovementComponent.class);
      if (movement != null) {
        movement.disableChargeAttacks();
        movement.setMode(FinalBossMovementComponent.Mode.STOPPED);
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
