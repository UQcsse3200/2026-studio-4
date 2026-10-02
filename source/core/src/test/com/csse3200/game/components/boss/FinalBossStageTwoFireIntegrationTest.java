package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
  private Entity boss;
  private Entity player;
  private GameTime time;
  private World world;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    config = new FinalBossStageTwoConfig();
    config.iceCoverCount = 0; // This fixture isolates fire/player/wall behaviour from random cover.
    playerStats = new CombatStatsComponent(100, 10);
    player = new Entity().addComponent(playerStats).addComponent(new PhysicsComponent());
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
  void normalAttacksStillWorkAndTheSixtyPercentHandoverClearsAllFire() {
    startEncounter();
    advance(config.fireballInitialDelay);
    fire.impacts.add(new FinalBossStageTwoFireController.Impact(new Vector2(10f, 10f)));
    assertFalse(fire.fireballs.isEmpty());

    bossStats.hit(playerStats);
    assertEquals(790, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    bossStats.takeDamage(10000, player);

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

    assertEquals(100 - config.fireballDamage, playerStats.getHealth());
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
  void playerPhysicsPositionIsRefreshedBeforeSweepingThePlayersMovement() {
    player.setPosition(7.5f, 9.5f);
    startEncounter();
    fire.fireballs.add(
        new FinalBossStageTwoFireController.Fireball(100L, new Vector2(10f, 10f), new Vector2()));
    // Physics has moved the player across the fireball before its entity earlyUpdate is called.
    player.getComponent(PhysicsComponent.class).getBody().setTransform(11.5f, 9.5f, 0f);

    advance(0.1f);

    assertEquals(100 - config.fireballDamage, playerStats.getHealth());
    assertTrue(fire.fireballs.isEmpty());
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

  private void assertFireCleared() {
    assertTrue(fire.fireballs.isEmpty());
    assertTrue(fire.impacts.isEmpty());
    assertEquals(0f, fire.getCastRemaining());
  }
}
