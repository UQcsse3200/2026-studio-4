package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.entities.factories.FinalBossFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class FinalBossStageOneEntranceTest {
  private final List<Entity> spawned = new ArrayList<>();
  private FinalBossStageOneComponent controller;
  private FinalBossStageOneConfig config;
  private FinalBossMovementComponent movement;
  private CombatStatsComponent stats;
  private FinalBossPhaseControllerComponent phase;
  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    config = new FinalBossStageOneConfig();
    stats = new CombatStatsComponent(100, 0);
    movement = mock(FinalBossMovementComponent.class);
    phase = new FinalBossPhaseControllerComponent();
    FinalBossDamageControllerComponent damage = new FinalBossDamageControllerComponent();
    controller = new FinalBossStageOneComponent(new Entity(), spawned::add, config);
    new Entity()
        .addComponent(stats)
        .addComponent(movement)
        .addComponent(phase)
        .addComponent(damage)
        .addComponent(controller);
    phase.create();
    damage.create();
    controller.create();
  }

  @Test
  void shouldWaitForTransformationAndCastBeforeStartingCombat() {
    try (MockedStatic<FinalBossFactory> factory = mockFactory()) {
      assertEquals(FinalBossStageOneState.INTRO, controller.getState());
      advance(config.bossIntroDuration);
      assertEquals(FinalBossStageOneState.TRANSFORMING, controller.getState());
      advance(config.bossTransformDuration);
      assertEquals(FinalBossStageOneState.SUMMONING_ONE, controller.getState());
      advance(config.bossSummonCastDuration / 2f);
      assertTrue(spawned.isEmpty());
      verify(movement, never()).setMode(FinalBossMovementComponent.Mode.STEP_TOWARDS_PLAYER);
      advance(config.bossSummonCastDuration / 2f);
      assertEquals(config.waveOneSummonCount, spawned.size());
      assertEquals(FinalBossStageOneState.WAVE_ONE, controller.getState());
      verify(movement).setMode(FinalBossMovementComponent.Mode.STEP_TOWARDS_PLAYER);
      advance(20f);
      assertEquals(config.waveOneSummonCount, spawned.size());
    }
  }

  @Test
  void shouldKeepFullVulnerabilityWindowAndFinishStageOnlyOnce() {
    try (MockedStatic<FinalBossFactory> factory = mockFactory()) {
      enterWaveOne();
      removeAllSummons();
      assertEquals(FinalBossStageOneState.BREAK_WINDOW, controller.getState());
      assertFalse(stats.isInvulnerable());
      advance(config.breakWindowDuration - 0.1f);
      assertEquals(FinalBossStageOneState.BREAK_WINDOW, controller.getState());
      advance(0.11f);
      assertEquals(FinalBossStageOneState.SUMMONING_TWO, controller.getState());
      assertTrue(stats.isInvulnerable());
      assertEquals(0, controller.getActiveSummonCount());
      advance(config.bossSummonCastDuration);
      assertEquals(config.waveTwoSummonCount, controller.getActiveSummonCount());
      removeAllSummons();
      assertEquals(FinalBossStageOneState.COMPLETE, controller.getState());
      assertEquals(80, stats.getHealth());
      assertEquals(FinalBossPhase.STAGE_TWO, phase.getCurrentPhase());
      removeAllSummons();
      advance(20f);
      assertEquals(FinalBossPhase.STAGE_TWO, phase.getCurrentPhase());
    }
  }

  @Test
  void shouldNotSpawnAfterDisposalDuringEntrance() {
    try (MockedStatic<FinalBossFactory> factory = mockFactory()) {
      controller.dispose();
      advance(20f);
      assertTrue(spawned.isEmpty());
    }
  }

  private MockedStatic<FinalBossFactory> mockFactory() {
    MockedStatic<FinalBossFactory> factory = mockStatic(FinalBossFactory.class);
    factory
        .when(() -> FinalBossFactory.createExplosiveSummon(any(), any(), anyFloat(), anyFloat()))
        .thenAnswer(invocation -> new Entity());
    return factory;
  }

  private void enterWaveOne() {
    advance(config.bossIntroDuration);
    advance(config.bossTransformDuration);
    advance(config.bossSummonCastDuration);
  }

  private void removeAllSummons() {
    for (Entity summon : new ArrayList<>(spawned)) {
      summon.getEvents().trigger(FinalBossEvents.SUMMON_REMOVED, summon);
    }
  }

  private void advance(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    controller.update();
  }
}
