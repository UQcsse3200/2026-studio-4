package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.PlayerPetrificationComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Exercises the Boss event contract through the player's real status effect controller. */
@ExtendWith(GameExtension.class)
class FinalBossPetrificationStatusTest {
  private static final float EPSILON = 0.001f;

  private GameTime time;
  private long now;
  private Entity player;
  private Body body;
  private CombatStatsComponent stats;
  private StatusEffectsControllerComponent effects;
  private PlayerPetrificationComponent petrification;
  private FinalBossStageOneConfig config;
  private EventHandler bossEvents;
  private FinalBossPetrificationComponent bossPetrification;
  private FinalBossPetrificationWarningRenderComponent warning;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> now);
    ServiceLocator.registerTimeSource(time);

    PhysicsComponent physics = mock(PhysicsComponent.class);
    body = mock(Body.class);
    when(physics.getBody()).thenReturn(body);
    when(body.getLinearVelocity()).thenReturn(new Vector2());
    when(body.getWorldCenter()).thenReturn(new Vector2());
    when(body.getMass()).thenReturn(1f);

    stats = new CombatStatsComponent(100, 10, 3f, 1f);
    effects = new StatusEffectsControllerComponent();
    petrification = new PlayerPetrificationComponent();
    player =
        new Entity()
            .addComponent(physics)
            .addComponent(stats)
            .addComponent(effects)
            .addComponent(petrification)
            .addComponent(new PlayerActions());
    player.create();

    config = new FinalBossStageOneConfig();
    FinalBossPhaseControllerComponent phases = mock(FinalBossPhaseControllerComponent.class);
    FinalBossStageOneComponent stageOne = mock(FinalBossStageOneComponent.class);
    warning = new FinalBossPetrificationWarningRenderComponent();
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_ONE);
    when(stageOne.getState()).thenReturn(FinalBossStageOneState.WAVE_TWO);
    bossEvents = new EventHandler();
    Entity boss = mock(Entity.class);
    when(boss.getEvents()).thenReturn(bossEvents);
    when(boss.getComponent(FinalBossPhaseControllerComponent.class)).thenReturn(phases);
    when(boss.getComponent(FinalBossStageOneComponent.class)).thenReturn(stageOne);
    when(boss.getComponent(FinalBossPetrificationWarningRenderComponent.class)).thenReturn(warning);
    bossPetrification = new FinalBossPetrificationComponent(player, config);
    bossPetrification.setEntity(boss);
    bossPetrification.create();
  }

  @Test
  void leavingWarningAreaShouldAvoidTheStatusEffect() {
    tickBoss(0.01f);
    player.setPosition(10f, 0f);
    tickBoss(config.petrificationWarningDuration);

    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed(), EPSILON);
  }

  @Test
  void warningShouldRemainHarmlessUntilItEnds() {
    tickBoss(0.01f);
    assertTrue(warning.isVisible());
    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed(), EPSILON);

    tickBoss(0.25f);
    tickBoss(0.125f);
    assertTrue(warning.isVisible());
    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed(), EPSILON);

    tickBoss(0.125f);
    assertFalse(warning.isVisible());
    assertTrue(petrification.isPetrified());
    assertEquals(1.5f, stats.getEffectiveMovementSpeed(), EPSILON);
  }

  @Test
  void dodgingShouldStillGiveTheFullCooldownBeforeAnotherWarning() {
    tickBoss(0.01f);
    player.setPosition(10f, 0f);
    tickBoss(config.petrificationWarningDuration);

    tickBoss(2.49f);
    assertFalse(warning.isVisible());
    assertFalse(petrification.isPetrified());
    tickBoss(0.02f);
    assertTrue(warning.isVisible());
    assertEquals(player.getCenterPosition(), bossPetrification.getMarkedPosition());
  }

  @Test
  void leavingBeforeResolutionShouldStaySafeEvenIfPlayerReturnsAfterward() {
    tickBoss(0.01f);
    Vector2 marked = bossPetrification.getMarkedPosition();
    tickBoss(0.25f);
    player.setPosition(10f, 0f);
    tickBoss(0.25f);
    assertEquals(marked, bossPetrification.getMarkedPosition());
    assertFalse(warning.isVisible());
    assertFalse(petrification.isPetrified());

    player.setPosition(0f, 0f);
    tickBoss(0.1f);
    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed(), EPSILON);
  }

  @Test
  void returningBeforeWarningEndsShouldApplyPenaltyOnlyAtExpiry() {
    tickBoss(0.01f);
    player.setPosition(10f, 0f);
    tickBoss(0.25f);
    player.setPosition(0f, 0f);
    tickBoss(0.125f);
    assertTrue(warning.isVisible());
    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed(), EPSILON);

    tickBoss(0.125f);
    assertFalse(warning.isVisible());
    assertTrue(petrification.isPetrified());
    assertEquals(1.5f, stats.getEffectiveMovementSpeed(), EPSILON);
  }

  private void tickBoss(float seconds) {
    now += Math.round(seconds * 1000f);
    when(time.getDeltaTime()).thenReturn(seconds);
    bossPetrification.update();
  }

}
