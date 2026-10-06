package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;

/** Fire controller integration with stage transitions, combat events and real wall fixtures. */
@ExtendWith(GameExtension.class)
class FinalBossStageTwoFireIntegrationTest {
  private FinalBossStageTwoConfig config;
  private FinalBossStageTwoComponent stageTwo;
  private FinalBossStageTwoFireController fire;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossStageTwoArenaComponent arena;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private StatusEffectsControllerComponent playerEffects;
  private ConsumableEffectComponent consumables;
  private InventoryComponent inventory;
  private Entity boss;
  private Entity player;
  private GameTime time;
  private World world;
  private long nextTestFireballId;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    config = new FinalBossStageTwoConfig();
    config.fireballDamage = 2f; // Keep collision tests independent of fractional-damage balancing.
    config.iceCoverCount = 0; // This fixture isolates fire/player/wall behaviour from random cover.
    playerStats = new CombatStatsComponent(100, 10);
    playerEffects = spy(new StatusEffectsControllerComponent());
    consumables = new ConsumableEffectComponent();
    inventory = new InventoryComponent(0);
    player =
        new Entity()
            .addComponent(playerStats)
            .addComponent(playerEffects)
            .addComponent(inventory)
            .addComponent(consumables)
            .addComponent(new PhysicsComponent());
    player.setPosition(79.5f, 79.5f);
    player.create();
    phases = new FinalBossPhaseControllerComponent();
    arena = mock(FinalBossStageTwoArenaComponent.class);
    when(arena.getBounds()).thenReturn(new Rectangle(0f, 0f, 100f, 100f));
    bossStats = new CombatStatsComponent(1000, 0);
    stageTwo = new FinalBossStageTwoComponent(player, config);
    boss =
        new Entity()
            .addComponent(bossStats)
            .addComponent(phases)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(mock(FinalBossMovementComponent.class))
            .addComponent(arena)
            .addComponent(stageTwo);
    boss.setPosition(49.5f, 49.5f);
    boss.create();
    bossStats.setHealth(800);
    fire = stageTwo.getFireController();
    nextTestFireballId = 10000L;
  }

  @AfterEach
  void disposeWorld() {
    world.dispose();
  }

  @Test
  void transformationDoesNotFireOrConsumeTheInitialFiringDelay() {
    phases.completeStage(FinalBossPhase.STAGE_ONE);
    advance(3f);
    assertTrue(fire.fireballs.isEmpty());

    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    assertFalse(phases.isTransitioning());
    advance(config.fireballInitialDelay / 2f);
    assertTrue(fire.fireballs.isEmpty());
    advance(config.fireballInitialDelay / 2f);
    assertFalse(fire.fireballs.isEmpty());
    assertTrue(fire.getCastRemaining() > 0f);
  }

  @Test
  void crossingIntoPauseMovesExistingBallsWithoutFiringAnotherVolley() {
    config.attackDuration = 1f;
    config.fireballSpeed = 0.5f;
    config.fireballLifetime = 50f;
    startEncounter();
    advance(0.75f);
    List<FinalBossStageTwoFireController.Fireball> original = new ArrayList<>(fire.fireballs);
    assertFalse(original.isEmpty());
    List<Vector2> positions = original.stream().map(ball -> ball.position.cpy()).toList();

    advance(0.5f);

    assertFalse(stageTwo.isAttacking());
    assertEquals(original, fire.fireballs);
    for (int index = 0; index < original.size(); index++) {
      assertEquals(0.25f, positions.get(index).dst(original.get(index).position), 0.0001f);
    }
    advance(2.5f);
    assertFalse(stageTwo.isAttacking());
    assertEquals(original, fire.fireballs);
    advance(0.25f);
    assertTrue(stageTwo.isAttacking());
    advance(0.01f);
    assertTrue(fire.fireballs.size() > original.size());
  }

  @Test
  void normalAttacksAreBlockedAndAnIceHitAtSixtyPercentClearsAllFire() {
    startEncounter();
    advance(config.fireballInitialDelay);
    fire.impacts.add(new FinalBossStageTwoFireController.Impact(new Vector2(10f, 10f)));
    assertFalse(fire.fireballs.isEmpty());

    bossStats.hit(playerStats);
    bossStats.takeDamage(10000, player);
    assertEquals(800, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    boss.getComponent(FinalBossDamageControllerComponent.class)
        .takeStageTwoIceDamage(10000, player);

    assertEquals(600, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertTrue(phases.isTransitioning());
    assertTrue(bossStats.isInvulnerable());
    assertFireCleared();
    advance(10f);
    assertFireCleared();
  }

  @Test
  void playerDeathClearsProjectilesBeforeAnotherShotCanHit() {
    startEncounter();
    advance(config.fireballInitialDelay);
    fire.impacts.add(new FinalBossStageTwoFireController.Impact(new Vector2(10f, 10f)));
    playerStats.setHealth(0);

    advance(0.1f);

    assertFireCleared();
    advance(10f);
    assertFireCleared();
  }

  @Test
  void lethalFireballClearsTheRemainingVolleyDuringTheDamageCallback() {
    config.fireballDamage = 100;
    player.setPosition(10.5f, 9.5f);
    startEncounter();
    fire.fireballs.add(
        new FinalBossStageTwoFireController.Fireball(
            100L, new Vector2(10f, 10f), new Vector2(4f, 0f)));
    fire.fireballs.add(
        new FinalBossStageTwoFireController.Fireball(
            101L, new Vector2(10f, 10f), new Vector2(4f, 0f)));
    AtomicInteger damagingHits = new AtomicInteger();
    player
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity attacker, Integer lost, Integer left) -> damagingHits.incrementAndGet());

    advance(0.3f);

    assertTrue(playerStats.isDead());
    assertEquals(1, damagingHits.get());
    assertFireCleared();
  }

  @Test
  void bossDeathClearsFireImmediatelyAndDoesNotRespawnIt() {
    startEncounter();
    advance(config.fireballInitialDelay);
    assertFalse(fire.fireballs.isEmpty());

    bossStats.setHealth(0);

    assertFireCleared();
    advance(10f);
    assertFireCleared();
  }

  @Test
  void disposalClearsFireAndPreventsLaterUpdatesFromAttacking() {
    startEncounter();
    advance(config.fireballInitialDelay);
    assertFalse(fire.fireballs.isEmpty());
    fire.impacts.add(new FinalBossStageTwoFireController.Impact(new Vector2(10f, 10f)));

    stageTwo.dispose();

    assertFireCleared();
    advance(10f);
    assertFireCleared();
    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void losingTheArenaRemovesFireRatherThanLeavingInvisibleHazards() {
    startEncounter();
    advance(config.fireballInitialDelay);
    assertFalse(fire.fireballs.isEmpty());
    when(arena.getBounds()).thenReturn(null);

    advance(0.1f);

    assertFireCleared();
  }

  @Test
  void realStaticWallStopsAFastFireballBeforeThePlayerBehindIt() {
    player.setPosition(13.5f, 9.5f);
    addWall(12f, 10f, false);
    startEncounter();
    fire.fireballs.add(
        new FinalBossStageTwoFireController.Fireball(
            100L, new Vector2(10f, 10f), new Vector2(10f, 0f)));

    advance(0.5f);

    assertEquals(100, playerStats.getHealth());
    assertTrue(fire.fireballs.isEmpty());
    assertEquals(1, fire.impacts.size());
    assertEquals(11.8f, fire.impacts.getFirst().position.x, 0.0001f);
  }

  @Test
  void staticSensorsDoNotShieldThePlayerFromFireballs() {
    player.setPosition(13.5f, 9.5f);
    addWall(12f, 10f, true);
    startEncounter();
    fire.fireballs.add(
        new FinalBossStageTwoFireController.Fireball(
            100L, new Vector2(10f, 10f), new Vector2(10f, 0f)));

    advance(0.5f);

    assertEquals(100 - config.fireballDamage, (float) playerStats.getHealth());
    assertTrue(fire.fireballs.isEmpty());
  }

  @Test
  void bossInsideARealWallCannotSpawnAVolleyThroughItsSurface() {
    Body wall = addWall(50f, 50f, false);
    startEncounter();

    advance(config.fireballInitialDelay);

    assertFireCleared();
    world.destroyBody(wall);
    advance(config.fireVolleyInterval);
    assertFalse(fire.fireballs.isEmpty());
  }

  @Test
  void changingBossScaleChangesTheRadiusOfItsNextVolley() {
    boss.setScale(2f, 2f);
    boss.setPosition(49f, 49f);
    startEncounter();
    advance(config.fireballInitialDelay);
    assertFalse(fire.fireballs.isEmpty());
    float firstRadius = FinalBossStageTwoFireGeometry.spawnRadius(boss.getScale());
    for (FinalBossStageTwoFireController.Fireball ball : fire.fireballs) {
      assertEquals(firstRadius, boss.getCenterPosition().dst(ball.position), 0.0001f);
    }

    List<Long> originalIds = fire.fireballs.stream().map(ball -> ball.id).toList();
    boss.setScale(4f, 2f);
    boss.setPosition(48f, 49f);
    advance(config.fireVolleyInterval);

    float resizedRadius = FinalBossStageTwoFireGeometry.spawnRadius(boss.getScale());
    assertTrue(resizedRadius > firstRadius);
    List<FinalBossStageTwoFireController.Fireball> newVolley =
        fire.fireballs.stream().filter(ball -> !originalIds.contains(ball.id)).toList();
    assertFalse(newVolley.isEmpty());
    for (FinalBossStageTwoFireController.Fireball ball : newVolley) {
      assertEquals(resizedRadius, boss.getCenterPosition().dst(ball.position), 0.0001f);
      assertEquals(0f, ball.elapsed);
    }
  }

  @Test
  void wallsBetweenTheBossAndItsEmissionRingCannotBeSkippedDuringSpawn() {
    boss.setScale(2f, 2f);
    boss.setPosition(49f, 49f);
    player.setPosition(52.5f, 49.5f);
    List<Body> walls =
        List.of(
            addWallBox(49f, 50f, 0.05f, 1.1f),
            addWallBox(51f, 50f, 0.05f, 1.1f),
            addWallBox(50f, 49f, 1.1f, 0.05f),
            addWallBox(50f, 51f, 1.1f, 0.05f));
    startEncounter();

    advance(config.fireballInitialDelay);

    assertTrue(fire.fireballs.isEmpty());
    assertTrue(fire.impacts.isEmpty());
    assertEquals(100, playerStats.getHealth());
    assertEquals(5, world.getBodyCount());
    for (Body wall : walls) world.destroyBody(wall);
    advance(config.fireVolleyInterval);
    assertFalse(fire.fireballs.isEmpty());
  }

  @Test
  void playerPhysicsPositionIsRefreshedBeforeSweepingThePlayersMovement() {
    player.setPosition(7.5f, 9.5f);
    startEncounter();
    fire.fireballs.add(
        new FinalBossStageTwoFireController.Fireball(100L, new Vector2(10f, 10f), new Vector2()));
    // Physics has moved the player across the fireball before its entity earlyUpdate is called.
    player.getComponent(PhysicsComponent.class).getBody().setTransform(11.5f, 9.5f, 0f);

    advance(0.1f);

    assertEquals(100 - config.fireballDamage, (float) playerStats.getHealth());
    assertTrue(fire.fireballs.isEmpty());
  }

  @Test
  void fourHalfDamageHitsConsumeEveryShotAndImpactButRemoveOnlyTwoHealth() {
    startHalfDamageEncounter();
    List<Entity> damageSources = new ArrayList<>();
    player
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity attacker, Integer lost, Integer left) -> damageSources.add(attacker));

    for (int expectedHealth : new int[] {100, 99, 99, 98}) {
      hitWithFireball();
      assertEquals(expectedHealth, playerStats.getHealth());
    }

    assertTrue(fire.fireballs.isEmpty());
    assertEquals(4, fire.impacts.size());
    assertEquals(List.of(boss, boss), damageSources);
  }

  @Test
  void halfDamageRemainderSurvivesNewVolleysAndTheFiringPause() {
    config.fireballDamage = 0.5f;
    config.fireballInitialDelay = 0.02f;
    config.fireballSpeed = 0.01f;
    config.attackDuration = 0.2f;
    config.pauseDuration = 0.2f;
    startEncounter();
    hitWithFireball();
    assertEquals(100, playerStats.getHealth());
    advance(0.02f);
    List<Long> firstVolley = fire.fireballs.stream().map(ball -> ball.id).toList();
    assertFalse(firstVolley.isEmpty());
    advance(0.18f);
    assertFalse(stageTwo.isAttacking());

    hitWithFireball();
    assertEquals(99, playerStats.getHealth());
    hitWithFireball();
    assertEquals(99, playerStats.getHealth());
    advance(0.19f);
    assertTrue(stageTwo.isAttacking());
    assertTrue(fire.fireballs.stream().anyMatch(ball -> !firstVolley.contains(ball.id)));
    hitWithFireball();

    assertEquals(98, playerStats.getHealth());
  }

  @Test
  void restartingTheEncounterDiscardsThePreviousHalfDamage() {
    startHalfDamageEncounter();
    hitWithFireball();
    assertEquals(100, playerStats.getHealth());

    stageTwo.startEncounter();

    assertFireCleared();
    hitWithFireball();
    assertEquals(100, playerStats.getHealth());
    hitWithFireball();
    assertEquals(99, playerStats.getHealth());
  }

  @Test
  void leavingTheStageClearsPendingHalfDamageAndPreventsAnyQueuedHit() {
    startHalfDamageEncounter();
    hitWithFireball();
    addFireballAtPlayer();

    phases.completeStage(FinalBossPhase.STAGE_TWO);
    advance(1f);

    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertEquals(100, playerStats.getHealth());
    assertFireCleared();
  }

  @Test
  void localZeroDamageNeitherAddsHalfDamageNorDiscardsAnEarlierEffectiveHalf() {
    assertBlockedHitsDoNotChangeRemainder(
        () -> playerStats.setIncomingDamageMultiplier(0f),
        () -> playerStats.setIncomingDamageMultiplier(1f));
  }

  @Test
  void invulnerabilityNeitherAddsHalfDamageNorDiscardsAnEarlierEffectiveHalf() {
    assertBlockedHitsDoNotChangeRemainder(
        () -> playerStats.setInvulnerable(true), () -> playerStats.setInvulnerable(false));
  }

  @Test
  void concealmentNeitherAddsHalfDamageNorDiscardsAnEarlierEffectiveHalf() {
    assertBlockedHitsDoNotChangeRemainder(
        () -> when(playerEffects.isConcealed()).thenReturn(true),
        () -> when(playerEffects.isConcealed()).thenReturn(false));
  }

  @Test
  void timedShieldPreservesEarlierHealthRemainderWithoutLeakingItsOwnHalfOnExpiry() {
    startHalfDamageEncounter();
    hitWithFireball();
    AtomicLong shieldTime = new AtomicLong();
    try (MockedConstruction<GameTime> ignored =
        mockConstruction(
            GameTime.class,
            (clock, context) -> when(clock.getTime()).thenAnswer(call -> shieldTime.get()))) {
      playerEffects.activateTimed();
      assertTrue(playerEffects.isShieldActive());
      hitWithFireball();
      hitWithFireball();
      hitWithFireball();
      assertEquals(100, playerStats.getHealth());

      shieldTime.set(3001L);
      playerEffects.update();
      assertFalse(playerEffects.isShieldActive());
      hitWithFireball();
      assertEquals(99, playerStats.getHealth());
      hitWithFireball();
      assertEquals(99, playerStats.getHealth());
      hitWithFireball();
      assertEquals(98, playerStats.getHealth());
    }
  }

  @Test
  void absorbShieldLosesOnePointPerPairAndItsRemainingHalfExpiresWithTheShield() {
    startHalfDamageEncounter();
    AtomicInteger shieldPoints = new AtomicInteger();
    player
        .getEvents()
        .addListener(
            "updateShield", (Integer current, Integer maximum) -> shieldPoints.set(current));
    AtomicLong shieldTime = new AtomicLong();
    try (MockedConstruction<GameTime> ignored =
        mockConstruction(
            GameTime.class,
            (clock, context) -> when(clock.getTime()).thenAnswer(call -> shieldTime.get()))) {
      playerEffects.activateAbsorb();
      assertTrue(playerEffects.isShieldActive());
      assertEquals(20, shieldPoints.get());

      hitWithFireball();
      assertEquals(20, shieldPoints.get());
      hitWithFireball();
      assertEquals(19, shieldPoints.get());
      hitWithFireball();
      assertEquals(19, shieldPoints.get());
      assertEquals(100, playerStats.getHealth());
      shieldTime.set(5001L);
      playerEffects.update();
      assertFalse(playerEffects.isShieldActive());
      hitWithFireball();
      assertEquals(100, playerStats.getHealth());
      hitWithFireball();
      assertEquals(99, playerStats.getHealth());
    }
  }

  @Test
  void consumableShieldNeitherAddsHalfDamageNorDiscardsEarlierEffectiveHalfOnExpiry() {
    startHalfDamageEncounter();
    inventory.addConsumable(ItemIds.SHIELD, 2);
    assertTrue(consumables.tryUse(ItemIds.SHIELD));
    long shieldDurationMs = consumables.getShieldRemainingMs();
    assertTrue(consumables.isShielded());
    hitWithFireball();
    assertEquals(100, playerStats.getHealth());

    when(time.getTime()).thenReturn(shieldDurationMs + 1);
    playerEffects.update();
    assertFalse(consumables.isShielded());
    hitWithFireball();
    assertEquals(100, playerStats.getHealth());
    assertTrue(consumables.tryUse(ItemIds.SHIELD));
    hitWithFireball();
    assertEquals(100, playerStats.getHealth());
    when(time.getTime()).thenReturn(2 * shieldDurationMs + 2);
    playerEffects.update();
    assertFalse(consumables.isShielded());
    hitWithFireball();

    assertEquals(99, playerStats.getHealth());
  }

  @Test
  void aHalfDamageLethalHitClearsTheRestOfItsVolleySynchronously() {
    startHalfDamageEncounter();
    playerStats.setHealth(1);
    hitWithFireball();
    assertEquals(1, playerStats.getHealth());
    addFireballAtPlayer();
    addFireballAtPlayer();
    AtomicInteger damagingHits = new AtomicInteger();
    player
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity attacker, Integer lost, Integer left) -> damagingHits.incrementAndGet());

    advance(0.01f);

    assertTrue(playerStats.isDead());
    assertEquals(1, damagingHits.get());
    assertFireCleared();
  }

  @Test
  void synchronousDisposalOnAWholeDamageTickDoesNotProcessTheRestOfTheVolley() {
    startHalfDamageEncounter();
    hitWithFireball();
    addFireballAtPlayer();
    addFireballAtPlayer();
    addFireballAtPlayer();
    AtomicInteger damagingHits = new AtomicInteger();
    player
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity attacker, Integer lost, Integer left) -> {
              damagingHits.incrementAndGet();
              stageTwo.dispose();
            });

    advance(0.01f);

    assertEquals(99, playerStats.getHealth());
    assertEquals(1, damagingHits.get());
    assertFireCleared();
    advance(1f);
    assertEquals(99, playerStats.getHealth());
    assertFireCleared();
  }

  @Test
  void positiveIntegerDamageStillAppliesFullyOnEveryHit() {
    config.fireballDamage = 3f;
    config.fireballInitialDelay = 1000f;
    startEncounter();

    hitWithFireball();
    assertEquals(97, playerStats.getHealth());
    hitWithFireball();
    assertEquals(94, playerStats.getHealth());
  }

  private void startHalfDamageEncounter() {
    config.fireballDamage = 0.5f;
    config.fireballInitialDelay = 1000f;
    startEncounter();
  }

  private void assertBlockedHitsDoNotChangeRemainder(Runnable block, Runnable unblock) {
    startHalfDamageEncounter();
    block.run();
    hitWithFireball();
    assertEquals(100, playerStats.getHealth());
    unblock.run();
    hitWithFireball();
    assertEquals(100, playerStats.getHealth());
    block.run();
    hitWithFireball();
    assertEquals(100, playerStats.getHealth());
    unblock.run();
    hitWithFireball();
    assertEquals(99, playerStats.getHealth());
  }

  private FinalBossStageTwoFireController.Fireball addFireballAtPlayer() {
    FinalBossStageTwoFireController.Fireball ball =
        new FinalBossStageTwoFireController.Fireball(
            nextTestFireballId++, player.getCenterPosition(), new Vector2());
    fire.fireballs.add(ball);
    return ball;
  }

  private void hitWithFireball() {
    FinalBossStageTwoFireController.Fireball ball = addFireballAtPlayer();
    advance(0.01f);
    assertFalse(fire.fireballs.contains(ball));
    assertFalse(fire.impacts.isEmpty());
    assertTrue(fire.impacts.getLast().position.epsilonEquals(player.getCenterPosition()));
    assertEquals(0f, fire.impacts.getLast().elapsed);
  }

  private void startEncounter() {
    phases.completeStage(FinalBossPhase.STAGE_ONE);
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    assertFalse(phases.isTransitioning());
  }

  private void advance(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stageTwo.update();
  }

  private Body addWall(float x, float y, boolean sensor) {
    BodyDef definition = new BodyDef();
    definition.type = BodyDef.BodyType.StaticBody;
    definition.position.set(x, y);
    Body body = world.createBody(definition);
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(0.2f, 2f);
    body.createFixture(shape, 0f).setSensor(sensor);
    shape.dispose();
    return body;
  }

  private Body addWallBox(float x, float y, float halfWidth, float halfHeight) {
    BodyDef definition = new BodyDef();
    definition.type = BodyDef.BodyType.StaticBody;
    definition.position.set(x, y);
    Body body = world.createBody(definition);
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(halfWidth, halfHeight);
    body.createFixture(shape, 0f);
    shape.dispose();
    return body;
  }

  private void assertFireCleared() {
    assertTrue(fire.fireballs.isEmpty());
    assertTrue(fire.impacts.isEmpty());
    assertEquals(0f, fire.getCastRemaining());
  }
}
