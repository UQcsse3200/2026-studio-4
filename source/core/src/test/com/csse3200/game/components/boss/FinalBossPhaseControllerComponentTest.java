package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
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
  void shouldStopMovementDuringTheStageTwoTransformation() {
    FinalBossMovementComponent movement = mock(FinalBossMovementComponent.class);
    FinalBossPhaseControllerComponent controller = new FinalBossPhaseControllerComponent();
    Entity boss = new Entity().addComponent(controller).addComponent(movement);
    boss.create();

    assertTrue(controller.completeStage(FinalBossPhase.STAGE_ONE));

    verify(movement).setMode(FinalBossMovementComponent.Mode.STOPPED);
    verify(movement, never()).enableChargeAttacks();
  }

  @Test
  void shouldStartTheStageTwoEncounterOnlyAfterTheTransition() {
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    FinalBossMovementComponent movement = mock(FinalBossMovementComponent.class);
    FinalBossStageTwoComponent stageTwo = mock(FinalBossStageTwoComponent.class);
    FinalBossPhaseControllerComponent controller = new FinalBossPhaseControllerComponent();
    new Entity().addComponent(controller).addComponent(movement).addComponent(stageTwo).create();
    controller.completeStage(FinalBossPhase.STAGE_ONE);

    when(time.getDeltaTime()).thenReturn(2.75f);
    controller.update();
    assertTrue(controller.isTransitioning());
    verify(stageTwo, never()).startEncounter();
    verify(movement, never()).enableChargeAttacks();

    when(time.getDeltaTime()).thenReturn(0.25f);
    controller.update();
    controller.update();

    assertFalse(controller.isTransitioning());
    verify(stageTwo).startEncounter();
    verify(movement, never()).enableChargeAttacks();
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
