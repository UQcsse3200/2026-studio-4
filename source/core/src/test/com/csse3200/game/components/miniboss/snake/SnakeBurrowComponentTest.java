package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.components.statuseffects.InvisibilityEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Gameplay regressions for the telegraphed burrow attack and its vulnerability window. */
@ExtendWith(GameExtension.class)
class SnakeBurrowComponentTest {
  private GameTime time;
  private SnakeMiniBossConfig config;
  private Entity snake;
  private Entity player;
  private CombatStatsComponent snakeStats;
  private CombatStatsComponent playerStats;
  private SnakeBurrowComponent burrow;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    config = new SnakeMiniBossConfig();
    playerStats = new CombatStatsComponent(100, 10);
    player =
        new Entity().addComponent(playerStats).addComponent(new StatusEffectsControllerComponent());
    player.setPosition(5f, 5f);
    player.create();

    snakeStats = new CombatStatsComponent(150, 10);
    burrow = new SnakeBurrowComponent(player, config);
    snake =
        new Entity()
            .addComponent(snakeStats)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(burrow);
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
  }

  @Test
  void stayingInsideTheWarningTakesOneLowDamageHit() {
    reachWarning();
    movePlayerCentre(burrow.getWarningCentre());
    tick(config.warningDuration);

    assertEquals(100 - config.burrowDamage, playerStats.getHealth());

    tick(0.5f);
    tick(0.5f);

    assertEquals(100 - config.burrowDamage, playerStats.getHealth());
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
