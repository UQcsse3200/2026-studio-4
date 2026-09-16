package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.PlayerPetrificationComponent;
import com.csse3200.game.components.statuseffects.LastStandEffect;
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
import org.mockito.ArgumentCaptor;

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
    FinalBossPetrificationWarningRenderComponent warning =
        mock(FinalBossPetrificationWarningRenderComponent.class);
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
  void warningHitShouldSlowMovementWithoutChangingRawStatsOrDamage() {
    hitPlayer();

    assertTrue(petrification.isPetrified());
    assertEquals(3f, stats.getMovementSpeed(), EPSILON);
    assertEquals(1.5f, stats.getEffectiveMovementSpeed(), EPSILON);
    assertEquals(10, stats.getEffectiveBaseAttack());
    assertEquals(1f, stats.getEffectiveAttackSpeed(), EPSILON);
    assertFalse(player.getComponent(PlayerActions.class).areControlsLocked());
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
  void expiryShouldRestoreSpeedAndPublishTheRestoredValue() {
    float[] displayedSpeed = {stats.getEffectiveMovementSpeed()};
    player
        .getEvents()
        .addListener("updateMovementSpeed", (Float speed) -> displayedSpeed[0] = speed);
    hitPlayer();
    assertEquals(1.5f, displayedSpeed[0], EPSILON);

    advanceEffects(1999);
    assertTrue(petrification.isPetrified());
    assertEquals(1.5f, stats.getEffectiveMovementSpeed(), EPSILON);

    advanceEffects(1);
    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed(), EPSILON);
    assertEquals(3f, displayedSpeed[0], EPSILON);
  }

  @Test
  void repeatedHitsShouldRefreshDurationInsteadOfCompoundingTheSlow() {
    hitPlayer();
    tickBoss(config.petrificationCooldown);
    tickBoss(config.petrificationWarningDuration);

    assertEquals(1.5f, stats.getEffectiveMovementSpeed(), EPSILON);
    // The first hit would now expire, but the second hit has another second left.
    advanceEffects(1000);
    assertTrue(petrification.isPetrified());
    assertEquals(1.5f, stats.getEffectiveMovementSpeed(), EPSILON);

    advanceEffects(1000);
    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed(), EPSILON);
  }

  @Test
  void leavingWaveTwoShouldImmediatelyRestoreSpeed() {
    hitPlayer();

    bossEvents.trigger(FinalBossEvents.STAGE_ONE_STATE_CHANGED, FinalBossStageOneState.COMPLETE);

    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed(), EPSILON);
    advanceEffects(3000);
    assertEquals(3f, stats.getEffectiveMovementSpeed(), EPSILON);
  }

  @Test
  void disposingTheBossShouldClearOnlyItsPetrificationEffect() {
    LastStandEffect otherEffect = new LastStandEffect(time, 10000);
    effects.addStatusEffect(otherEffect);
    hitPlayer();
    assertEquals(2.25f, stats.getEffectiveMovementSpeed(), EPSILON);

    bossPetrification.dispose();

    assertFalse(petrification.isPetrified());
    assertTrue(effects.hasStatusEffect(otherEffect));
    assertEquals(4.5f, stats.getEffectiveMovementSpeed(), EPSILON);
  }

  @Test
  void speedChangesMadeDuringPetrificationShouldSurviveItsExpiry() {
    LastStandEffect otherEffect = new LastStandEffect(time, 10000);
    effects.addStatusEffect(otherEffect);
    hitPlayer();
    stats.addMovementSpeed(1f);

    assertEquals(4f, stats.getMovementSpeed(), EPSILON);
    assertEquals(3f, stats.getEffectiveMovementSpeed(), EPSILON);
    advanceEffects(2000);

    assertEquals(4f, stats.getMovementSpeed(), EPSILON);
    assertEquals(6f, stats.getEffectiveMovementSpeed(), EPSILON);
    assertTrue(effects.hasStatusEffect(otherEffect));
  }

  @Test
  void playerDeathShouldClearTheEffectAndRejectFurtherRequests() {
    hitPlayer();
    stats.setHealth(0);

    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed(), EPSILON);
    requestEffect();
    assertFalse(petrification.isPetrified());
  }

  @Test
  void disposedControllerShouldClearTheEffectAndRejectFurtherRequests() {
    hitPlayer();
    effects.dispose();

    requestEffect();

    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed(), EPSILON);
  }

  @Test
  void walkingShouldUseTheSlowedSpeedAndRecoverAfterExpiry() {
    hitPlayer();
    PlayerActions actions = player.getComponent(PlayerActions.class);
    player.getEvents().trigger("walk", new Vector2(1f, 0f));
    actions.update();
    advanceEffects(2000);
    actions.update();

    ArgumentCaptor<Vector2> impulses = ArgumentCaptor.forClass(Vector2.class);
    verify(body, times(2)).applyLinearImpulse(impulses.capture(), any(Vector2.class), eq(true));
    assertEquals(1.5f, impulses.getAllValues().get(0).x, EPSILON);
    assertEquals(3f, impulses.getAllValues().get(1).x, EPSILON);
  }

  private void hitPlayer() {
    tickBoss(0.01f);
    tickBoss(config.petrificationWarningDuration);
  }

  private void tickBoss(float seconds) {
    now += Math.round(seconds * 1000f);
    when(time.getDeltaTime()).thenReturn(seconds);
    bossPetrification.update();
  }

  private void advanceEffects(long milliseconds) {
    now += milliseconds;
    effects.update();
  }

  private void requestEffect() {
    player
        .getEvents()
        .trigger(
            FinalBossEvents.PETRIFICATION_EFFECT_REQUESTED,
            config.petrificationSlowMultiplier,
            config.petrificationSlowDuration);
  }
}
