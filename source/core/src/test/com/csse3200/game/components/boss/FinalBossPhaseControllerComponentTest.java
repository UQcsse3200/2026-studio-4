package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossPhaseControllerComponentTest {
  @Test
  void shouldAdvanceOnlyTheActivePhase() {
    FinalBossPhaseControllerComponent controller = new FinalBossPhaseControllerComponent();
    Entity boss = new Entity().addComponent(controller);
    boss.create();

    assertFalse(controller.completeStage(FinalBossPhase.STAGE_TWO));
    assertTrue(controller.completeStage(FinalBossPhase.STAGE_ONE));
    assertFalse(controller.completeStage(FinalBossPhase.STAGE_ONE));
    assertEquals(FinalBossPhase.STAGE_TWO, controller.getCurrentPhase());
  }

  @Test
  void shouldReachDefeatedSequentially() {
    FinalBossPhaseControllerComponent controller = new FinalBossPhaseControllerComponent();
    Entity boss = new Entity().addComponent(controller);
    boss.create();

    boss.getEvents().trigger(FinalBossEvents.STAGE_COMPLETED, FinalBossPhase.STAGE_ONE);
    boss.getEvents().trigger(FinalBossEvents.STAGE_COMPLETED, FinalBossPhase.STAGE_TWO);
    boss.getEvents().trigger(FinalBossEvents.STAGE_COMPLETED, FinalBossPhase.STAGE_THREE);

    assertEquals(FinalBossPhase.DEFEATED, controller.getCurrentPhase());
    assertFalse(controller.completeStage(FinalBossPhase.DEFEATED));
  }
}
