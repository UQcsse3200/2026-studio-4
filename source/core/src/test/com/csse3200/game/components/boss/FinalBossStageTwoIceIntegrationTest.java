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
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Real Stage 2 fire sweeps against ice fixtures, player combat and queued entity disposal. */
@ExtendWith(GameExtension.class)
class FinalBossStageTwoIceIntegrationTest {
  private FinalBossStageTwoConfig config;
  private FinalBossStageTwoComponent stageTwo;
  private FinalBossStageTwoFireController fire;
  private FinalBossStageTwoIceController ice;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossStageTwoArenaComponent arena;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private Entity player;
  private Entity boss;
  private EntityService entities;
  private GameTime time;
  private World world;
  private long nextFireballId;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    config = new FinalBossStageTwoConfig();
    config.fireballDamage = 2f; // Keep collision tests independent of fractional-damage balancing.
    config.iceCoverCount = 1;
    config.fireballInitialDelay = 1000f;
    playerStats = new CombatStatsComponent(100, 10);
    player =
        new Entity()
            .addComponent(playerStats)
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.PLAYER));
    player.setPosition(24.5f, 24.5f);
    player.create();
    phases = new FinalBossPhaseControllerComponent();
    arena = mock(FinalBossStageTwoArenaComponent.class);
    when(arena.getBounds()).thenReturn(new Rectangle(0f, 0f, 40f, 40f));
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
    boss.setPosition(14.5f, 14.5f);
    boss.create();
    bossStats.setHealth(800);
    fire = stageTwo.getFireController();
    ice = stageTwo.getIceController();
    nextFireballId = 1000L;
  }

  @AfterEach
  void disposeWorld() {
    world.dispose();
  }

  @Test
  void initialCoverSpawnsInsideTheArenaWithoutOverlappingEitherActor() {
    startEncounter();

    assertEquals(1, ice.covers.size());
    Rectangle cover = ice.covers.getFirst().bounds;
    assertTrue(new Rectangle(0f, 0f, 40f, 40f).contains(cover));
    assertFalse(cover.overlaps(actorBounds(player)));
    assertFalse(cover.overlaps(actorBounds(boss)));
    assertEquals(2, world.getBodyCount());
    assertEquals(0, entities.getEntities().size);
  }

  @Test
  void coverBeforeThePlayerConsumesTheShotAndLosesExactlyOneHit() {
    FinalBossStageTwoIceController.Cover cover = prepareCoverAndPlayerBehind();
    addShotsThrough(cover, 1);

    advance(0.3f);

    assertEquals(3, cover.hitsRemaining);
    assertEquals(100, playerStats.getHealth());
    assertTrue(fire.fireballs.isEmpty());
    assertTrue(ice.shatters.isEmpty());
  }

  @Test
  void playerBeforeTheCoverTakesDamageWithoutDamagingTheCover() {
    FinalBossStageTwoIceController.Cover cover = prepareCoverAndPlayerBehind();
    placePlayerAt(cover.bounds.x - 1f, centreY(cover));
    addShotsThrough(cover, 1);

    advance(0.3f);

    assertEquals(100 - config.fireballDamage, playerStats.getHealth());
    assertEquals(4, cover.hitsRemaining);
    assertTrue(fire.fireballs.isEmpty());
  }

  @Test
  void aCoverHitDoesNotCountTowardThePlayersTwoRequiredHalfDamageHits() {
    config.fireballDamage = 0.5f;
    FinalBossStageTwoIceController.Cover cover = prepareCoverAndPlayerBehind();
    addShotsThrough(cover, 1);
    advance(0.3f);
    assertEquals(3, cover.hitsRemaining);
    assertEquals(100, playerStats.getHealth());

    placePlayerAt(cover.bounds.x - 1f, centreY(cover));
    addShotsThrough(cover, 1);
    advance(0.3f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(3, cover.hitsRemaining);
    addShotsThrough(cover, 1);
    advance(0.3f);

    assertEquals(99, playerStats.getHealth());
    assertEquals(3, cover.hitsRemaining);
    assertTrue(fire.fireballs.isEmpty());
  }

  @Test
  void aWallBeforeTheCoverConsumesTheShotWithoutDamagingIceBehindIt() {
    FinalBossStageTwoIceController.Cover cover = prepareCoverAndPlayerBehind();
    addWall(cover.bounds.x - 1f, centreY(cover));
    addShotsThrough(cover, 1);

    advance(0.3f);

    assertEquals(4, cover.hitsRemaining);
    assertEquals(100, playerStats.getHealth());
    assertTrue(fire.fireballs.isEmpty());
  }

  @Test
  void checkingAnOriginInsideIcePreventsShotsWithoutCountingAsAHit() {
    config.fireballInitialDelay = 0.6f;
    FinalBossStageTwoIceController.Cover cover = prepareCoverAndPlayerBehind();
    boss.setPosition(cover.bounds.x + cover.bounds.width / 2f - 0.5f, centreY(cover) - 0.5f);

    advance(config.fireballInitialDelay);

    assertTrue(fire.fireballs.isEmpty());
    assertEquals(4, cover.hitsRemaining);
    assertTrue(ice.shatters.isEmpty());
  }

  @Test
  void coverBetweenTheBossAndItsEmissionRingBlocksSpawnWithoutTakingFireballDamage() {
    config.fireballInitialDelay = 0.6f;
    config.iceCoverGap = 0f;
    config.iceCoverHeight = 2.2f;
    boss.setScale(2f, 2f);
    startEncounter();
    FinalBossStageTwoIceController.Cover cover = ice.covers.getFirst();
    float y = centreY(cover);
    boss.setPosition(cover.bounds.x - 2.01f, y - 1f);
    player.setPosition(cover.bounds.x + cover.bounds.width + 0.25f, y - 0.5f);
    // The 1.55-radius ring exceeds the arena's 1.12 half-height. Its allowed rightward rays
    // cross the ice 1.01 units from the origin; leftward rays exceed the 1.015 left clearance.
    // Both actors still fit inside the arena, and the boss stops 0.01 units before the cover.
    when(arena.getBounds())
        .thenReturn(
            new Rectangle(
                cover.bounds.x - 2.025f,
                cover.bounds.y - 0.02f,
                5.025f,
                cover.bounds.height + 0.04f));
    advance(0.001f);

    advance(config.fireballInitialDelay);

    assertTrue(fire.fireballs.isEmpty());
    assertTrue(fire.impacts.isEmpty());
    assertEquals(config.iceCoverHits, cover.hitsRemaining);
    assertEquals(1, ice.covers.size());
    assertTrue(ice.shatters.isEmpty());
    assertEquals(100, playerStats.getHealth());
    assertEquals(2, world.getBodyCount());
  }

  @Test
  void fourthShotShattersTheCoverAndFifthShotInTheSameFrameHitsThePlayer() {
    FinalBossStageTwoIceController.Cover cover = prepareCoverAndPlayerBehind();
    addShotsThrough(cover, 5);

    advance(0.3f);

    assertTrue(ice.covers.isEmpty());
    assertEquals(1, ice.shatters.size());
    assertEquals(100 - config.fireballDamage, playerStats.getHealth());
    assertTrue(fire.fireballs.isEmpty());
    assertEquals(1, world.getBodyCount());
    assertEquals(0, entities.getEntities().size);
  }

  @Test
  void existingFireballsStillDamageCoverDuringTheFiringPause() {
    config.attackDuration = 1f;
    FinalBossStageTwoIceController.Cover cover = prepareCoverAndPlayerBehind();
    advance(1f);
    assertFalse(stageTwo.isAttacking());
    addShotsThrough(cover, 1);

    advance(0.3f);

    assertFalse(stageTwo.isAttacking());
    assertEquals(3, cover.hitsRemaining);
    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void coverExpiresDuringTheFiringPauseAndNoLongerBlocksShotsAtItsOldPosition() {
    config.attackDuration = 1f;
    config.pauseDuration = 10f;
    config.iceCoverRespawnInterval = 100f;
    FinalBossStageTwoIceController.Cover cover = prepareCoverAndPlayerBehind();
    advance(6.9f);
    assertFalse(stageTwo.isAttacking());
    assertEquals(1, ice.covers.size());
    assertEquals(2, world.getBodyCount());
    addShotsThrough(cover, 1);

    // The shot reaches the old ice surface just after its seven-second lifetime ends.
    advance(0.3f);

    assertFalse(stageTwo.isAttacking());
    assertTrue(ice.covers.isEmpty());
    assertTrue(ice.shatters.isEmpty());
    assertEquals(4, cover.hitsRemaining);
    assertEquals(1, world.getBodyCount());
    assertEquals(100 - config.fireballDamage, playerStats.getHealth());
    assertTrue(fire.fireballs.isEmpty());
  }

  @Test
  void localPlayerInvulnerabilityDoesNotMakeIceCoverIndestructible() {
    FinalBossStageTwoIceController.Cover cover = prepareCoverAndPlayerBehind();
    playerStats.setIncomingDamageMultiplier(0f);
    addShotsThrough(cover, 5);

    advance(0.3f);

    assertTrue(ice.covers.isEmpty());
    assertEquals(1, ice.shatters.size());
    assertEquals(100, playerStats.getHealth());
    assertTrue(fire.fireballs.isEmpty());
  }

  @Test
  void aRealPlayerColliderCannotDashThroughIceCover() {
    FinalBossStageTwoIceController.Cover cover = prepareCoverAndPlayerBehind();
    placePlayerAt(cover.bounds.x - 1.5f, centreY(cover));
    Body playerBody = player.getComponent(PhysicsComponent.class).getBody();
    playerBody.setLinearDamping(0f);

    for (int frame = 0; frame < 60; frame++) {
      playerBody.setLinearVelocity(15f, 0f);
      world.step(1f / 60f, 6, 2);
    }
    player.getComponent(PhysicsComponent.class).earlyUpdate();

    assertTrue(player.getPosition().x + player.getScale().x <= cover.bounds.x + 0.04f);
    assertEquals(4, cover.hitsRemaining);
  }

  @Test
  void phaseExitRemovesCoverBodiesAndOutstandingShatterEffects() {
    prepareCoverAndShatter();

    boss.getComponent(FinalBossDamageControllerComponent.class)
        .takeStageTwoIceDamage(10000, player);

    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertEncounterResourcesCleared();
  }

  @Test
  void playerDeathRemovesCoverBodiesAndOutstandingShatterEffects() {
    prepareCoverAndShatter();

    playerStats.setHealth(0);
    advance(0.01f);

    assertEncounterResourcesCleared();
  }

  @Test
  void bossDeathRemovesCoverBodiesAndOutstandingShatterEffects() {
    prepareCoverAndShatter();

    bossStats.setHealth(0);

    assertEncounterResourcesCleared();
  }

  @Test
  void disposalRemovesCoverBodiesAndOutstandingShatterEffects() {
    prepareCoverAndShatter();

    stageTwo.dispose();

    assertEncounterResourcesCleared();
  }

  @Test
  void losingTheArenaRemovesCoverBodiesAndOutstandingShatterEffects() {
    prepareCoverAndShatter();
    when(arena.getBounds()).thenReturn(null);

    advance(0.01f);

    assertEncounterResourcesCleared();
  }

  private void startEncounter() {
    phases.completeStage(FinalBossPhase.STAGE_ONE);
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    assertFalse(phases.isTransitioning());
    advance(0.01f);
    assertFalse(ice.covers.isEmpty());
  }

  private FinalBossStageTwoIceController.Cover prepareCoverAndPlayerBehind() {
    startEncounter();
    FinalBossStageTwoIceController.Cover cover = ice.covers.getFirst();
    // Keep the generated cover but allow shots on either side regardless of its random spawn.
    when(arena.getBounds()).thenReturn(new Rectangle(-100f, -100f, 200f, 200f));
    placePlayerAt(cover.bounds.x + cover.bounds.width + 2f, centreY(cover));
    return cover;
  }

  private void prepareCoverAndShatter() {
    config.iceCoverCount = 2;
    startEncounter();
    FinalBossStageTwoIceController.Cover cover = ice.covers.getFirst();
    when(arena.getBounds()).thenReturn(new Rectangle(-100f, -100f, 200f, 200f));
    placePlayerAt(80f, 80f);
    // Hit the selected cover from just outside its own surface so a second random cover cannot
    // intercept these cleanup-setup shots. The normal collision-order tests use a longer sweep.
    for (int shot = 0; shot < 4; shot++) {
      fire.fireballs.add(
          new FinalBossStageTwoFireController.Fireball(
              nextFireballId++,
              new Vector2(cover.bounds.x - 0.01f, centreY(cover)),
              new Vector2(20f, 0f)));
    }
    advance(0.05f);
    assertEquals(1, ice.covers.size());
    assertEquals(1, ice.shatters.size());
    assertEquals(2, world.getBodyCount());
  }

  private void placePlayerAt(float centreX, float centreY) {
    player.setPosition(centreX - 0.5f, centreY - 0.5f);
    // Refresh the component's previous player position before adding shots to the world.
    advance(0.001f);
  }

  private void addShotsThrough(FinalBossStageTwoIceController.Cover cover, int count) {
    for (int shot = 0; shot < count; shot++) {
      fire.fireballs.add(
          new FinalBossStageTwoFireController.Fireball(
              nextFireballId++,
              new Vector2(cover.bounds.x - 2f, centreY(cover)),
              new Vector2(20f, 0f)));
    }
  }

  private float centreY(FinalBossStageTwoIceController.Cover cover) {
    return cover.bounds.y + cover.bounds.height / 2f;
  }

  private Rectangle actorBounds(Entity actor) {
    return new Rectangle(
        actor.getPosition().x, actor.getPosition().y, actor.getScale().x, actor.getScale().y);
  }

  private void advance(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stageTwo.update();
    entities.update();
  }

  private void assertEncounterResourcesCleared() {
    entities.update();
    assertTrue(ice.covers.isEmpty());
    assertTrue(ice.shatters.isEmpty());
    assertTrue(fire.fireballs.isEmpty());
    assertEquals(1, world.getBodyCount());
    assertEquals(0, entities.getEntities().size);
  }

  private void addWall(float x, float y) {
    BodyDef definition = new BodyDef();
    definition.type = BodyDef.BodyType.StaticBody;
    definition.position.set(x, y);
    Body wall = world.createBody(definition);
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(0.1f, 2f);
    wall.createFixture(shape, 0f);
    shape.dispose();
  }
}
