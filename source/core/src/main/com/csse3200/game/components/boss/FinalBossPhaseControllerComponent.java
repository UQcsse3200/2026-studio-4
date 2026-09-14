package com.csse3200.game.components.boss;

import com.csse3200.game.components.Component;

/** Controls sequential transitions between the three Final Boss stages. */
public class FinalBossPhaseControllerComponent extends Component {
  private FinalBossPhase currentPhase = FinalBossPhase.STAGE_ONE;

  @Override
  public void create() {
    entity.getEvents().addListener(FinalBossEvents.STAGE_COMPLETED, this::completeStage);
  }

  public FinalBossPhase getCurrentPhase() {
    return currentPhase;
  }

  public boolean completeStage(FinalBossPhase completedPhase) {
    if (completedPhase != currentPhase || currentPhase == FinalBossPhase.DEFEATED) {
      return false;
    }

    currentPhase = nextPhase(currentPhase);
    if (currentPhase == FinalBossPhase.STAGE_TWO) {
      FinalBossMovementComponent movement = entity.getComponent(FinalBossMovementComponent.class);
      if (movement != null) {
        movement.setMode(FinalBossMovementComponent.Mode.STEP_TOWARDS_PLAYER);
        movement.enableChargeAttacks();
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
