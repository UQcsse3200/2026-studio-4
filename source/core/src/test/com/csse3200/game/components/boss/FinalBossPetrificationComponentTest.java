package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossPetrificationComponentTest {
  private Entity player;
  private Entity boss;
  private EventHandler bossEvents;

  private FinalBossStageOneConfig config;
  private FinalBossPhaseControllerComponent phaseController;
  private FinalBossStageOneComponent stageOneController;
  private FinalBossPetrificationWarningRenderComponent warningRenderer;
  private FinalBossPetrificationComponent component;

  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);

    config = new FinalBossStageOneConfig();

    player = new Entity();
    player.setPosition(0f, 0f);

    phaseController = mock(FinalBossPhaseControllerComponent.class);
    stageOneController = mock(FinalBossStageOneComponent.class);
    warningRenderer = mock(FinalBossPetrificationWarningRenderComponent.class);

    when(phaseController.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_ONE);
    when(stageOneController.getState()).thenReturn(FinalBossStageOneState.WAVE_TWO);

    bossEvents = new EventHandler();

    boss = mock(Entity.class);
    when(boss.getEvents()).thenReturn(bossEvents);
    when(boss.getComponent(FinalBossPhaseControllerComponent.class)).thenReturn(phaseController);
    when(boss.getComponent(FinalBossStageOneComponent.class)).thenReturn(stageOneController);
    when(boss.getComponent(FinalBossPetrificationWarningRenderComponent.class))
        .thenReturn(warningRenderer);

    component = new FinalBossPetrificationComponent(player, config);
    component.setEntity(boss);
    component.create();
  }

  @Test
  void shouldHitPlayerWhoRemainsInsideWarningArea() {
    int[] hitCount = {0};
    int[] effectRequestCount = {0};
    float[] requestedMultiplier = {0f};
    float[] requestedDuration = {0f};

    bossEvents.addListener(FinalBossEvents.PETRIFICATION_HIT, (Entity target) -> hitCount[0]++);

    player
        .getEvents()
        .addListener(
            FinalBossEvents.PETRIFICATION_EFFECT_REQUESTED,
            (Float multiplier, Float duration) -> {
              effectRequestCount[0]++;
              requestedMultiplier[0] = multiplier;
              requestedDuration[0] = duration;
            });

    // First Wave 2 update immediately creates the warning.
    advance(0.01f);

    assertTrue(component.isWarningActive());

    // Player does not move before the warning resolves.
    advance(config.petrificationWarningDuration);

    assertFalse(component.isWarningActive());
    assertEquals(1, hitCount[0]);
    assertEquals(1, effectRequestCount[0]);
    assertEquals(config.petrificationSlowMultiplier, requestedMultiplier[0], 0.001f);
    assertEquals(config.petrificationSlowDuration, requestedDuration[0], 0.001f);
  }

  @Test
  void shouldMissPlayerWhoLeavesWarningAreaBeforeActivation() {
    int[] hitCount = {0};
    int[] missCount = {0};
    int[] effectRequestCount = {0};

    bossEvents.addListener(FinalBossEvents.PETRIFICATION_HIT, (Entity target) -> hitCount[0]++);

    bossEvents.addListener(FinalBossEvents.PETRIFICATION_MISSED, markedPosition -> missCount[0]++);

    player
        .getEvents()
        .addListener(
            FinalBossEvents.PETRIFICATION_EFFECT_REQUESTED,
            (Float multiplier, Float duration) -> effectRequestCount[0]++);

    advance(0.01f);

    assertTrue(component.isWarningActive());

    // Move well outside the fixed warning circle after it has been placed.
    player.setPosition(10f, 0f);

    advance(config.petrificationWarningDuration);

    assertFalse(component.isWarningActive());
    assertEquals(0, hitCount[0]);
    assertEquals(1, missCount[0]);
    assertEquals(0, effectRequestCount[0]);
  }

  @Test
  void shouldRespectCooldownBetweenPetrificationWarnings() {
    // Begin first warning.
    advance(0.01f);
    assertTrue(component.isWarningActive());

    // Resolve it.
    advance(config.petrificationWarningDuration);

    assertFalse(component.isWarningActive());
    assertEquals(config.petrificationCooldown, component.getCooldownRemaining(), 0.001f);

    // Almost all of the cooldown passes, but another warning must not start yet.
    advance(config.petrificationCooldown - 0.1f);

    assertFalse(component.isWarningActive());
    assertTrue(component.getCooldownRemaining() > 0f);

    // Once the remaining cooldown expires, a new warning should start.
    advance(0.2f);

    assertTrue(component.isWarningActive());
    verify(warningRenderer, times(2))
        .showAt(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(config.petrificationRadius));
  }

  @Test
  void shouldImmediatelyClearWarningWhenStageOneEnds() {
    int[] clearRequestCount = {0};

    player
        .getEvents()
        .addListener(
            FinalBossEvents.PETRIFICATION_EFFECT_CLEAR_REQUESTED, () -> clearRequestCount[0]++);

    // Start an active warning.
    advance(0.01f);

    assertTrue(component.isWarningActive());

    // StageOneComponent emits COMPLETE immediately before Stage 2 starts.
    bossEvents.trigger(FinalBossEvents.STAGE_ONE_STATE_CHANGED, FinalBossStageOneState.COMPLETE);

    // Cleanup must happen immediately without waiting for another update().
    assertFalse(component.isWarningActive());
    assertEquals(1, clearRequestCount[0]);
    verify(warningRenderer, times(1)).hide();
  }

  private void advance(float deltaTime) {
    when(time.getDeltaTime()).thenReturn(deltaTime);
    component.update();
  }
}
