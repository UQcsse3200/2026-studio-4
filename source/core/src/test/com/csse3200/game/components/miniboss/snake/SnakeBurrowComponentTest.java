package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.PlayerDamageFlashComponent;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.components.statuseffects.InvisibilityEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;

/** Gameplay regressions for the telegraphed burrow attack and its vulnerability window. */
@ExtendWith(GameExtension.class)
class SnakeBurrowComponentTest {
  private GameTime time;
  private SnakeMiniBossConfig config;
  private Entity snake;
  private Entity player;
  private CombatStatsComponent snakeStats;
  private CombatStatsComponent playerStats;
  private StatusEffectsControllerComponent playerEffects;
  private SnakeBurrowComponent burrow;
  private SnakePlayerHitVisualComponent hitVisual;
  private SnakePoisonVolleyComponent poison;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(new RenderService());
    config = new SnakeMiniBossConfig();
    playerStats = new CombatStatsComponent(100, 10);
    playerEffects = new StatusEffectsControllerComponent();
    player =
        new Entity()
            .addComponent(playerStats)
            .addComponent(playerEffects)
            .addComponent(new PlayerDamageFlashComponent());
    player.setPosition(5f, 5f);
    player.create();

    snakeStats = new CombatStatsComponent(150, 10);
    burrow = new SnakeBurrowComponent(player, config);
    hitVisual = new SnakePlayerHitVisualComponent(player);
    poison = mock(SnakePoisonVolleyComponent.class);
    snake =
        new Entity()
            .addComponent(snakeStats)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(burrow)
            .addComponent(hitVisual)
            .addComponent(poison);
    snake.setPosition(3f, 5f);
    snake.create();
    tick(0f);
  }

  @Test
  void burrowingBlocksBothWeaponAndDirectDamage() {
    snakeStats.hit(playerStats);
    snakeStats.takeDamage(20);

    assertEquals(SnakeBurrowComponent.State.BURROWING, burrow.getState());
    assertEquals(150, snakeStats.getHealth());
  }

  @Test
  void undergroundMovementAndWarningKeepSnakeInvulnerable() {
    tick(config.burrowDuration);
    snakeStats.takeDamage(20);
    assertEquals(SnakeBurrowComponent.State.UNDERGROUND, burrow.getState());
    assertEquals(150, snakeStats.getHealth());

    tick(config.undergroundDuration);
    snakeStats.hit(playerStats);
    assertEquals(SnakeBurrowComponent.State.WARNING, burrow.getState());
    assertEquals(150, snakeStats.getHealth());
  }

  @Test
  void warningLocksItsPositionWhenPlayerMoves() {
    reachWarning();
    Vector2 locked = burrow.getWarningCentre().cpy();

    player.setPosition(15f, 15f);
    tick(config.warningDuration / 2f);

    assertEquals(locked, burrow.getWarningCentre());
    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void warningPositionCannotBeChangedByItsCaller() {
    reachWarning();
    Vector2 locked = burrow.getWarningCentre().cpy();

    burrow.getWarningCentre().set(-100f, -100f);

    assertEquals(locked, burrow.getWarningCentre());
  }

  @Test
  void dodgingOutsideTheWarningAvoidsTheAttack() {
    reachWarning();
    movePlayerCentre(burrow.getWarningCentre().cpy().add(burrow.getWarningRadius() + 0.1f, 0f));

    tick(config.warningDuration);

    assertEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
    assertEquals(100, playerStats.getHealth());
    assertFalse(hitVisual.isPlaying());
    assertNull(playerEffects.getTint());
  }

  @Test
  void stayingInsideTheWarningTakesOneHitWithGreenAndRedFeedback() {
    reachWarning();
    movePlayerCentre(burrow.getWarningCentre());
    tick(config.warningDuration);

    assertEquals(100 - config.burrowDamage, playerStats.getHealth());
    assertTrue(hitVisual.isPlaying());
    assertEquals(new Color(1f, 0.2f, 0.2f, 1f), playerEffects.getTint());

    when(time.getTime()).thenReturn(600L);
    tick(0.5f);
    assertFalse(hitVisual.isPlaying());
    assertNull(playerEffects.getTint());

    when(time.getTime()).thenReturn(1000L);
    tick(0.5f);

    assertEquals(100 - config.burrowDamage, playerStats.getHealth());
    assertFalse(hitVisual.isPlaying());
    assertNull(playerEffects.getTint());
  }

  @Test
  void walkingIntoTheCircleAfterTheStrikeDoesNotDealContactDamage() {
    reachWarning();
    Vector2 locked = burrow.getWarningCentre().cpy();
    movePlayerCentre(locked.cpy().add(3f, 0f));
    tick(config.warningDuration);

    movePlayerCentre(locked);
    tick(0.5f);

    assertEquals(100, playerStats.getHealth());
    assertFalse(hitVisual.isPlaying());
    assertNull(playerEffects.getTint());
  }

  @Test
  void attackReportsSnakeAsTheDamageSource() {
    Entity[] source = {null};
    player
        .getEvents()
        .addListener(
            "damageTaken", (Entity attacker, Integer lost, Integer left) -> source[0] = attacker);
    reachWarning();
    movePlayerCentre(burrow.getWarningCentre());

    tick(config.warningDuration);

    assertSame(snake, source[0]);
  }

  @Test
  void localPlayerDamageMultiplierStillAppliesToTheBurrowAttack() {
    playerStats.setIncomingDamageMultiplier(0f);
    reachWarning();
    movePlayerCentre(burrow.getWarningCentre());

    tick(config.warningDuration);

    assertEquals(100, playerStats.getHealth());
    assertFalse(hitVisual.isPlaying());
    assertNull(playerEffects.getTint());
  }

  @Test
  void invulnerablePlayerDoesNotReceiveHitFeedback() {
    playerStats.setInvulnerable(true);
    reachExposed();

    assertEquals(100, playerStats.getHealth());
    assertFalse(hitVisual.isPlaying());
    assertNull(playerEffects.getTint());
  }

  @Test
  void fullyAbsorbedAttackDoesNotReceiveHitFeedback() {
    playerEffects.activateTimed();
    reachExposed();

    assertEquals(100, playerStats.getHealth());
    assertFalse(hitVisual.isPlaying());
    assertNull(playerEffects.getTint());
  }

  @Test
  void lethalStrikeDoesNotRestartFeedbackAfterPlayerDeath() {
    playerStats.setHealth(config.burrowDamage);
    reachExposed();

    assertEquals(0, playerStats.getHealth());
    assertFalse(hitVisual.isPlaying());
    assertNull(playerEffects.getTint());
  }

  @Test
  void snakeCanBeDamagedDuringTheFullOneAndAHalfSecondExposedWindow() {
    reachExposed();
    snakeStats.takeDamage(10);
    tick(1.49f);
    snakeStats.hit(playerStats);

    assertEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
    assertFalse(snakeStats.isInvulnerable());
    assertEquals(130, snakeStats.getHealth());

    tick(0.02f);
    snakeStats.takeDamage(10);

    assertEquals(SnakeBurrowComponent.State.BURROWING, burrow.getState());
    assertTrue(snakeStats.isInvulnerable());
    assertEquals(130, snakeStats.getHealth());
  }

  @Test
  void aLongFrameCannotSkipTheWarningAndImmediatelyDamagePlayer() {
    tick(100f);
    assertEquals(SnakeBurrowComponent.State.UNDERGROUND, burrow.getState());
    assertEquals(100, playerStats.getHealth());

    tick(100f);
    assertEquals(SnakeBurrowComponent.State.WARNING, burrow.getState());
    assertEquals(100, playerStats.getHealth());

    tick(config.warningDuration / 2f);
    assertEquals(SnakeBurrowComponent.State.WARNING, burrow.getState());
    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void freezingDuringWarningPausesTheAttackUntilThawed() {
    reachWarning();
    movePlayerCentre(burrow.getWarningCentre());
    float before = burrow.getStateTime();
    snake
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(new FrozenEffect(time, 1000L));

    tick(10f);

    assertEquals(SnakeBurrowComponent.State.WARNING, burrow.getState());
    assertEquals(before, burrow.getStateTime());
    assertEquals(100, playerStats.getHealth());

    when(time.getTime()).thenReturn(1000L);
    tick(config.warningDuration);

    assertEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
    assertEquals(100 - config.burrowDamage, playerStats.getHealth());
  }

  @Test
  void concealedPlayerIsNeitherTrackedNorAttacked() {
    tick(config.burrowDuration);
    player
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(new InvisibilityEffect(time, 1000L));
    Vector2 before = snake.getPosition();
    player.setPosition(100f, 100f);

    tick(10f);
    tick(10f);

    assertEquals(before, snake.getPosition());
    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void becomingConcealedDuringWarningPreventsTheStrike() {
    reachWarning();
    movePlayerCentre(burrow.getWarningCentre());
    player
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(new InvisibilityEffect(time, 1000L));

    tick(config.warningDuration);
    tick(config.exposedDuration);

    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void dyingDuringWarningCancelsTheAttackPermanently() {
    reachWarning();
    snakeStats.setHealth(0);

    tick(config.warningDuration);
    tick(100f);

    assertEquals(SnakeBurrowComponent.State.DEAD, burrow.getState());
    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void dyingDuringExposedWindowDoesNotStartAnotherBurrow() {
    reachExposed();
    snakeStats.takeDamage(1000, player);

    tick(config.exposedDuration);
    tick(100f);

    assertEquals(SnakeBurrowComponent.State.DEAD, burrow.getState());
    assertEquals(0, snakeStats.getHealth());
  }

  @Test
  void deadPlayerDoesNotReceiveFurtherAttackAttempts() {
    reachWarning();
    playerStats.setHealth(0);
    int[] attempts = {0};
    player
        .getEvents()
        .addListener("damageAttempted", (Integer damage, Entity attacker) -> attempts[0]++);

    tick(config.warningDuration);
    tick(100f);

    assertEquals(0, attempts[0]);
  }

  @Test
  void arenaBoundsKeepTheWarningInsideThePlayableRoom() {
    Rectangle room = new Rectangle(0f, 0f, 8f, 8f);
    burrow.setArenaBounds(room);
    player.setPosition(100f, 100f);

    reachWarning();

    assertTrue(room.contains(burrow.getWarningCentre()));
  }

  @Test
  void physicsIsInactiveUndergroundAndRestoredWhenSnakeEmerges() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    PhysicsComponent physics = new PhysicsComponent();
    SnakeBurrowComponent physicalBurrow = new SnakeBurrowComponent(player, config);
    Entity physicalSnake =
        new Entity()
            .addComponent(physicalBurrow)
            .addComponent(new CombatStatsComponent(150, 10))
            .addComponent(physics)
            .addComponent(new PhysicsMovementComponent());
    physicalSnake.create();
    when(time.getDeltaTime()).thenReturn(0f);
    physicalBurrow.update();

    assertFalse(physics.getBody().isActive());

    when(time.getDeltaTime()).thenReturn(config.burrowDuration);
    physicalBurrow.update();
    when(time.getDeltaTime()).thenReturn(config.undergroundDuration);
    physicalBurrow.update();
    when(time.getDeltaTime()).thenReturn(config.warningDuration);
    physicalBurrow.update();

    assertEquals(SnakeBurrowComponent.State.EXPOSED, physicalBurrow.getState());
    assertTrue(physics.getBody().isActive());
  }

  @Test
  void healthAboveHalfKeepsTheOriginalBurrowCycle() {
    snakeStats.setHealth(76);
    reachExposed();

    tick(config.exposedDuration);

    assertFalse(burrow.isStageTwo());
    assertEquals(SnakeBurrowComponent.State.BURROWING, burrow.getState());
    verify(poison, never()).beginSpit();
    verify(poison, never()).fireVolley(anyInt());
  }

  @Test
  void reachingHalfHealthPreservesTheWarningAndFullRecoveryWindow() {
    reachWarning();
    snakeStats.setHealth(75);

    tick(config.warningDuration / 2f);

    assertTrue(burrow.isStageTwo());
    assertEquals(SnakeBurrowComponent.State.WARNING, burrow.getState());
    verify(poison, never()).beginSpit();

    tick(config.warningDuration / 2f);
    tick(1.49f);

    assertEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
    verify(poison, never()).beginSpit();

    tick(0.02f);

    assertEquals(SnakeBurrowComponent.State.SPITTING, burrow.getState());
    assertFalse(snakeStats.isInvulnerable());
    verify(poison).beginSpit();
    verify(poison, never()).fireVolley(anyInt());
  }

  @Test
  void stageTwoUsesMaximumHealthRatioAndRemainsLatchedAfterHealing() {
    reachExposed();
    snakeStats.setMaxHealth(200);
    snakeStats.setHealth(101);
    tick(0.1f);
    assertFalse(burrow.isStageTwo());

    snakeStats.setHealth(100);
    tick(0.1f);
    assertTrue(burrow.isStageTwo());

    snakeStats.setHealth(200);
    tick(config.exposedDuration);

    assertTrue(burrow.isStageTwo());
    assertEquals(SnakeBurrowComponent.State.SPITTING, burrow.getState());
    verify(poison).beginSpit();
  }

  @Test
  void spittingKeepsSnakeVulnerable() {
    reachSpitting();

    snakeStats.takeDamage(10, player);

    assertFalse(snakeStats.isInvulnerable());
    assertEquals(65, snakeStats.getHealth());
    assertEquals(SnakeBurrowComponent.State.SPITTING, burrow.getState());
  }

  @Test
  void fiveOrderedVolleysFinishBeforeTheNextBurrowAndSpitCycle() {
    reachSpitting();
    tick(config.spitWindupDuration - 0.01f);
    verify(poison, never()).fireVolley(anyInt());

    tick(0.02f);
    for (int volley = 1; volley < 5; volley++) {
      tick(config.spitVolleyInterval);
      assertEquals(SnakeBurrowComponent.State.SPITTING, burrow.getState());
    }
    tick(config.spitRecoveryDuration - 0.01f);
    assertEquals(SnakeBurrowComponent.State.SPITTING, burrow.getState());
    tick(0.02f);

    InOrder order = inOrder(poison);
    order.verify(poison).beginSpit();
    order.verify(poison).fireVolley(0);
    order.verify(poison).fireVolley(1);
    order.verify(poison).fireVolley(2);
    order.verify(poison).fireVolley(3);
    order.verify(poison).fireVolley(4);
    order.verify(poison).endSpit();
    verify(poison, times(5)).fireVolley(anyInt());
    assertEquals(SnakeBurrowComponent.State.BURROWING, burrow.getState());
    assertTrue(snakeStats.isInvulnerable());

    reachExposed();
    tick(config.exposedDuration);

    assertEquals(SnakeBurrowComponent.State.SPITTING, burrow.getState());
    verify(poison, times(2)).beginSpit();
  }

  @Test
  void aLongSpitFrameEmitsOnlyOneVolleyAndPreservesTheNextGap() {
    reachSpitting();

    tick(100f);

    verify(poison).fireVolley(0);
    verify(poison, times(1)).fireVolley(anyInt());
    tick(config.spitVolleyInterval - 0.01f);
    verify(poison, never()).fireVolley(1);

    tick(0.02f);

    verify(poison).fireVolley(1);
    verify(poison, times(2)).fireVolley(anyInt());
  }

  @Test
  void freezingPausesSpitWindupUntilSnakeThaws() {
    reachSpitting();
    tick(config.spitWindupDuration / 2f);
    float before = burrow.getStateTime();
    snake
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(new FrozenEffect(time, 1000L));

    tick(100f);

    assertEquals(before, burrow.getStateTime());
    verify(poison, never()).fireVolley(anyInt());

    when(time.getTime()).thenReturn(1000L);
    tick(config.spitWindupDuration / 2f);

    verify(poison).fireVolley(0);
  }

  @Test
  void concealmentPausesTheGapBetweenPoisonVolleys() {
    reachSpitting();
    tick(config.spitWindupDuration);
    playerEffects.addStatusEffect(new InvisibilityEffect(time, 1000L));
    float before = burrow.getStateTime();

    tick(100f);

    assertEquals(before, burrow.getStateTime());
    verify(poison, never()).fireVolley(1);

    when(time.getTime()).thenReturn(1000L);
    tick(config.spitVolleyInterval);

    verify(poison).fireVolley(1);
  }

  @Test
  void snakeDeathCancelsPoisonAndPreventsFurtherVolleys() {
    reachSpitting();
    tick(config.spitWindupDuration);
    clearInvocations(poison);

    snakeStats.setHealth(0);
    tick(100f);

    assertEquals(SnakeBurrowComponent.State.DEAD, burrow.getState());
    verify(poison).clear();
    verify(poison, never()).fireVolley(anyInt());
  }

  @Test
  void playerDeathCancelsPoisonAndPreventsFurtherVolleys() {
    reachSpitting();
    clearInvocations(poison);

    playerStats.setHealth(0);
    tick(100f);

    verify(poison).clear();
    verify(poison, never()).fireVolley(anyInt());
  }

  @Test
  void disposalCancelsPoisonAndPreventsFurtherVolleys() {
    reachSpitting();
    clearInvocations(poison);

    burrow.dispose();
    tick(100f);

    assertEquals(SnakeBurrowComponent.State.DEAD, burrow.getState());
    verify(poison).clear();
    verify(poison, never()).fireVolley(anyInt());
  }

  private void reachSpitting() {
    reachExposed();
    snakeStats.setHealth(75);
    tick(config.exposedDuration);
    assertEquals(SnakeBurrowComponent.State.SPITTING, burrow.getState());
  }

  private void reachWarning() {
    tick(config.burrowDuration);
    tick(config.undergroundDuration);
    assertEquals(SnakeBurrowComponent.State.WARNING, burrow.getState());
  }

  private void reachExposed() {
    reachWarning();
    tick(config.warningDuration);
    assertEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
  }

  private void movePlayerCentre(Vector2 centre) {
    player.setPosition(centre.cpy().mulAdd(player.getScale(), -0.5f));
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    burrow.update();
  }
}
