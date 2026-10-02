package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossStageTwoIceControllerTest {
  private final Rectangle arena = new Rectangle(0f, 0f, 24f, 20f);
  private PhysicsEngine physics;
  private World world;
  private EntityService entities;
  private Entity boss;
  private Entity player;
  private FinalBossStageTwoConfig config;
  private FinalBossStageTwoIceController ice;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.016f);
    ServiceLocator.registerTimeSource(time);
    physics = spy(new PhysicsEngine());
    world = physics.getWorld();
    ServiceLocator.registerPhysicsService(new PhysicsService(physics));
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    boss = new Entity();
    boss.setPosition(-100f, -100f);
    player = new Entity();
    player.setPosition(-200f, -200f);
    config = new FinalBossStageTwoConfig();
    ice = new FinalBossStageTwoIceController(boss, player, config, new Random(91));
  }

  @AfterEach
  void disposeWorld() {
    ice.dispose();
    entities.update();
    world.dispose();
  }

  @Test
  void initialPlacementLeavesPassagesAroundActorsWallsOtherCoverAndEdges() {
    boss.setPosition(5f, 5f);
    boss.setScale(2f, 3f);
    player.setPosition(16f, 12f);
    Rectangle wall = new Rectangle(11f, 0f, 1f, 20f);
    physicalEntity(wall, BodyType.StaticBody, false, PhysicsLayer.OBSTACLE);

    ice.update(0.1f, arena);

    assertEquals(config.iceCoverCount, ice.covers.size());
    assertEquals(0, entities.getEntities().size, "Cover is owned directly by the controller");
    Rectangle interior = expanded(arena, -config.iceCoverGap);
    for (var cover : ice.covers) {
      assertTrue(interior.contains(cover.bounds));
      Rectangle clearance = expanded(cover.bounds, config.iceCoverGap);
      assertFalse(clearance.overlaps(new Rectangle(5f, 5f, 2f, 3f)));
      assertFalse(clearance.overlaps(new Rectangle(16f, 12f, 1f, 1f)));
      assertFalse(clearance.overlaps(wall));
      assertNull(cover.entity.getComponent(CombatStatsComponent.class));
      assertEquals(
          BodyType.StaticBody,
          cover.entity.getComponent(PhysicsComponent.class).getBody().getType());
      assertEquals(
          PhysicsLayer.OBSTACLE, cover.entity.getComponent(ColliderComponent.class).getLayer());
      for (var other : ice.covers) {
        if (other != cover) assertFalse(clearance.overlaps(other.bounds));
      }
    }
  }

  @Test
  void prefersNearbyCoverOnTheBossSideOfThePlayer() {
    config.iceCoverCount = 1;
    player.setPosition(8f, 9.5f);
    boss.setPosition(18f, 9.5f);
    ice = new FinalBossStageTwoIceController(boss, player, config, midpointRandom());

    ice.update(0f, arena);

    assertEquals(1, ice.covers.size());
    Rectangle cover = ice.covers.getFirst().bounds;
    assertNearbyOnBossSide(cover);
    assertTrue(expanded(arena, -config.iceCoverGap).contains(cover));
    assertFalse(expanded(cover, config.iceCoverGap).overlaps(new Rectangle(8f, 9.5f, 1f, 1f)));
  }

  @Test
  void replenishmentUsesThePlayersNewPositionAndCurrentBossDirection() {
    config.iceCoverCount = 1;
    player.setPosition(4f, 5f);
    boss.setPosition(19f, 5f);
    ice = new FinalBossStageTwoIceController(boss, player, config, midpointRandom());
    ice.update(0f, arena);
    assertEquals(1, ice.covers.size());
    assertNearbyOnBossSide(ice.covers.getFirst().bounds);
    Vector2 oldPlayerCentre = player.getCenterPosition();
    breakAllCovers();

    player.setPosition(14f, 13f);
    boss.setPosition(3f, 13f);
    ice.update(config.iceCoverRespawnInterval, arena);

    assertEquals(1, ice.covers.size());
    Rectangle cover = ice.covers.getFirst().bounds;
    assertNearbyOnBossSide(cover);
    assertTrue(
        cover.getCenter(new Vector2()).dst(oldPlayerCentre) > config.iceCoverNearMaxDistance);
  }

  @Test
  void blockedBossSideFallsBackToNearbyCoverOnTheOtherSide() {
    config.iceCoverCount = 1;
    player.setPosition(10f, 9.5f);
    boss.setPosition(2f, 9.5f);
    Rectangle wall = new Rectangle(5.5f, 7f, 3f, 6f);
    physicalEntity(wall, BodyType.StaticBody, false, PhysicsLayer.OBSTACLE);
    ice = new FinalBossStageTwoIceController(boss, player, config, midpointRandom());

    ice.update(0f, arena);

    assertEquals(1, ice.covers.size());
    Rectangle cover = ice.covers.getFirst().bounds;
    Vector2 centre = cover.getCenter(new Vector2());
    assertTrue(centre.x > player.getCenterPosition().x);
    assertTrue(centre.dst(player.getCenterPosition()) <= config.iceCoverNearMaxDistance);
    assertTrue(centre.dst(player.getCenterPosition()) >= config.iceCoverNearMinDistance);
    assertFalse(expanded(cover, config.iceCoverGap).overlaps(wall));
    assertTrue(expanded(arena, -config.iceCoverGap).contains(cover));
  }

  @Test
  void boundaryRejectsEntireNearbyCoverAndFallsBackInsideTheArena() {
    config.iceCoverCount = 1;
    // The sampled nearby centre fits, but its full width would cross the inset arena edge.
    player.setPosition(18.5f, 9.5f);
    boss.setPosition(30f, 9.5f);
    ice = new FinalBossStageTwoIceController(boss, player, config, midpointRandom());

    ice.update(0f, arena);

    assertEquals(1, ice.covers.size());
    Rectangle cover = ice.covers.getFirst().bounds;
    assertTrue(expanded(arena, -config.iceCoverGap).contains(cover));
    assertTrue(
        cover.getCenter(new Vector2()).dst(player.getCenterPosition())
            > config.iceCoverNearMaxDistance);
    assertFalse(expanded(cover, config.iceCoverGap).overlaps(new Rectangle(18.5f, 9.5f, 1f, 1f)));
  }

  @Test
  void pickupClearanceFilterStillRejectsNearbyCoverBeforeArenaFallback() {
    config.iceCoverCount = 1;
    player.setPosition(18f, 9.5f);
    boss.setPosition(30f, 9.5f);
    Rectangle pickupArea = new Rectangle(17f, 7f, 7f, 6f);
    ice =
        new FinalBossStageTwoIceController(
            boss, player, config, midpointRandom(), area -> !area.overlaps(pickupArea));

    ice.update(0f, arena);

    assertEquals(1, ice.covers.size());
    Rectangle cover = ice.covers.getFirst().bounds;
    assertFalse(expanded(cover, config.iceCoverGap).overlaps(pickupArea));
    assertTrue(expanded(arena, -config.iceCoverGap).contains(cover));
    assertTrue(
        cover.getCenter(new Vector2()).dst(player.getCenterPosition())
            > config.iceCoverNearMaxDistance);
  }

  @Test
  void onlyExactOwnedFixtureLosesHealthAndFourthHitShattersOnce() {
    ice.update(0f, arena);
    var first = ice.covers.getFirst();
    var second = ice.covers.get(1);
    Fixture fixture = first.entity.getComponent(ColliderComponent.class).getFixture();
    Fixture foreign =
        physicalEntity(
                new Rectangle(-20f, -20f, 1f, 1f),
                BodyType.StaticBody,
                false,
                PhysicsLayer.OBSTACLE)
            .getComponent(ColliderComponent.class)
            .getFixture();
    assertFalse(ice.hitByFire(null));
    assertFalse(ice.hitByFire(foreign));
    for (int hit = 1; hit <= 3; hit++) {
      assertTrue(ice.hitByFire(fixture));
      assertEquals(4 - hit, first.hitsRemaining);
      assertTrue(ice.covers.contains(first));
      assertEquals(4, second.hitsRemaining);
      assertTrue(first.hitFlashRemaining > 0f);
    }
    ice.update(0.2f, arena);
    assertEquals(0f, first.hitFlashRemaining);
    int bodiesBefore = world.getBodyCount();

    assertTrue(ice.hitByFire(fixture));

    assertFalse(ice.covers.contains(first));
    assertEquals(bodiesBefore - 1, world.getBodyCount());
    assertEquals(1, ice.shatters.size());
    assertEquals(first.bounds, ice.shatters.getFirst().bounds);
    assertFalse(ice.hitByFire(fixture));
    assertEquals(1, ice.shatters.size());
    ice.update(config.iceShatterDuration, arena);
    assertTrue(ice.shatters.isEmpty());
  }

  @Test
  void replenishesTwoPerIntervalWithoutCatchUpOrExceedingCapacity() {
    // Keep existing cover alive to isolate the replenishment cap during the long frame.
    config.iceCoverCount = 6;
    config.iceCoverRespawnBatch = 2;
    config.iceCoverRespawnInterval = 2f;
    config.iceCoverLifetime = 1000f;
    ice.update(0.1f, arena);
    breakAllCovers();
    ice.update(config.iceCoverRespawnInterval - 0.1f, arena);
    assertTrue(ice.covers.isEmpty());
    ice.update(0.1f, arena);
    assertEquals(2, ice.covers.size());
    ice.update(600f, arena);
    assertEquals(4, ice.covers.size());
    ice.update(config.iceCoverRespawnInterval, arena);
    assertEquals(6, ice.covers.size());
    breakCover(ice.covers.getFirst());
    ice.update(config.iceCoverRespawnInterval, arena);
    assertEquals(6, ice.covers.size(), "Only the single missing slot can be replenished");
  }

  @Test
  void coverExpiresAtSevenSecondsIncludingArrivalAndHitsDoNotExtendItsLifetime() {
    config.iceCoverCount = 1;
    config.iceCoverRespawnInterval = 100f;
    ice.update(0f, arena);
    var cover = ice.covers.getFirst();

    ice.update(6.75f, arena);
    assertTrue(ice.covers.contains(cover));
    assertEquals(1, world.getBodyCount());
    assertTrue(ice.hitByFire(cover.entity.getComponent(ColliderComponent.class).getFixture()));
    assertEquals(3, cover.hitsRemaining);

    ice.update(0.25f, arena);

    assertTrue(ice.covers.isEmpty());
    assertEquals(0, world.getBodyCount(), "Expiration must remove the physical obstacle too");
    assertTrue(ice.shatters.isEmpty(), "Natural expiry should not prolong the visible effect");
  }

  @Test
  void replenishedCoverGetsItsOwnFullLifetime() {
    config.iceCoverCount = 2;
    config.iceCoverRespawnBatch = 1;
    config.iceCoverRespawnInterval = 2f;
    ice.update(0f, arena);
    var original = ice.covers.getFirst();
    breakCover(ice.covers.get(1));

    ice.update(2f, arena);
    var replenished = ice.covers.get(1);
    ice.update(5f, arena);

    assertFalse(ice.covers.contains(original));
    assertTrue(ice.covers.contains(replenished), "New cover is only five seconds old");
    assertTrue(replenished.entity.getComponent(PhysicsComponent.class).getBody().isActive());
    assertEquals(2, ice.covers.size());
    var newest = ice.covers.get(1);

    ice.update(2f, arena);

    assertFalse(ice.covers.contains(replenished));
    assertTrue(ice.covers.contains(newest), "One cover's expiry must not age its replacement");
    assertEquals(2, world.getBodyCount());
  }

  @Test
  void coverIsSolidDuringItsArrivalEffectAndRefilledCoverGetsANewEffect() {
    config.iceCoverCount = 1;
    ice.update(0f, arena);
    var cover = ice.covers.getFirst();
    assertEquals(0f, cover.spawnElapsed);
    assertTrue(cover.entity.getComponent(PhysicsComponent.class).getBody().isActive());
    assertTrue(ice.hitByFire(cover.entity.getComponent(ColliderComponent.class).getFixture()));
    assertEquals(3, cover.hitsRemaining);
    ice.update(config.iceSpawnDuration / 2f, arena);
    assertEquals(config.iceSpawnDuration / 2f, cover.spawnElapsed, 0.0001f);
    ice.update(config.iceSpawnDuration, arena);
    assertEquals(config.iceSpawnDuration, cover.spawnElapsed);
    Fixture fixture = cover.entity.getComponent(ColliderComponent.class).getFixture();
    while (cover.hitsRemaining > 0) assertTrue(ice.hitByFire(fixture));
    ice.update(config.iceCoverRespawnInterval, arena);
    assertEquals(1, ice.covers.size());
    assertEquals(0f, ice.covers.getFirst().spawnElapsed);
    ice.clear();
    assertTrue(ice.covers.isEmpty());
  }

  @Test
  void smallArenaAndOccupiedArenaSkipPlacementWithoutForcingOverlap() {
    ice.update(0f, new Rectangle(0f, 0f, 2f, 2f));
    assertTrue(ice.covers.isEmpty());
    ice.clear();
    physicalEntity(arena, BodyType.StaticBody, false, PhysicsLayer.OBSTACLE);
    ice.update(0f, arena);
    assertTrue(ice.covers.isEmpty());
    assertEquals(1, world.getBodyCount());
  }

  @Test
  void sensorsDoNotExcludePlacementButSolidStaticBodiesDo() {
    physicalEntity(arena, BodyType.StaticBody, true, PhysicsLayer.OBSTACLE);
    ice.update(0f, arena);
    assertEquals(config.iceCoverCount, ice.covers.size());
  }

  @Test
  void waitsForBothServicesBeforePerformingInitialSpawn() {
    ServiceLocator.registerPhysicsService(null);
    ice.update(1f, arena);
    assertTrue(ice.covers.isEmpty());
    ServiceLocator.registerPhysicsService(new PhysicsService(physics));
    ServiceLocator.registerEntityService(null);
    ice.update(1f, arena);
    assertTrue(ice.covers.isEmpty());
    ServiceLocator.registerEntityService(entities);
    ice.update(0f, arena);
    assertEquals(config.iceCoverCount, ice.covers.size());
  }

  @Test
  void zeroCoverCountNeverCreatesBodies() {
    config.iceCoverCount = 0;
    ice.update(0f, arena);
    ice.update(100f, arena);
    assertTrue(ice.covers.isEmpty());
    assertEquals(0, world.getBodyCount());
  }

  @Test
  void coverPhysicallyBlocksMovingPlayerCollider() {
    config.iceCoverCount = 1;
    ice.update(0f, arena);
    Rectangle cover = ice.covers.getFirst().bounds;
    Entity moving =
        physicalEntity(
            new Rectangle(cover.x - 1f, cover.y + 0.4f, 0.5f, 0.5f),
            BodyType.DynamicBody,
            false,
            PhysicsLayer.PLAYER);
    Body body = moving.getComponent(PhysicsComponent.class).getBody();
    float startX = body.getPosition().x;
    for (int step = 0; step < 100; step++) {
      body.setLinearVelocity(5f, 0f);
      world.step(PhysicsEngine.PHYSICS_TIMESTEP, 6, 2);
    }
    assertTrue(body.getPosition().x > startX + 0.1f);
    assertTrue(body.getPosition().x + 0.5f <= cover.x + 0.03f);
  }

  @Test
  void shrinkingArenaRemovesOutsideBodiesAndResizeClearsActorOverlapOnlyOnce() {
    config.iceCoverCount = 1;
    ice.update(0f, arena);
    var cover = ice.covers.getFirst();
    player.setPosition(cover.bounds.x, cover.bounds.y);
    ice.update(0f, new Rectangle(arena));
    assertEquals(1, ice.covers.size(), "Ordinary contact must not delete cover");
    ice.update(0f, new Rectangle(0f, 0f, arena.width + 1f, arena.height));
    assertTrue(ice.covers.isEmpty(), "Resize may clamp the player into surviving cover");
    assertEquals(0, world.getBodyCount());
    player.setPosition(-200f, -200f);
    ice.clear();
    ice.update(0f, arena);
    ice.update(0f, new Rectangle(0f, 0f, 2f, 2f));
    assertTrue(ice.covers.isEmpty());
    assertEquals(0, world.getBodyCount());
    assertTrue(ice.shatters.isEmpty(), "Resize cleanup is not an attack impact");
  }

  @Test
  void clearCanRestartButDisposeCannotAndBothAreIdempotent() {
    ice.update(0f, arena);
    breakCover(ice.covers.getFirst());
    ice.clear();
    ice.clear();
    assertTrue(ice.covers.isEmpty());
    assertTrue(ice.shatters.isEmpty());
    assertEquals(0, world.getBodyCount());
    ice.update(0f, arena);
    assertEquals(config.iceCoverCount, ice.covers.size());
    ice.dispose();
    ice.dispose();
    ice.update(100f, arena);
    assertTrue(ice.covers.isEmpty());
    assertEquals(0, world.getBodyCount());
  }

  @Test
  void fourthHitDuringContactDefersBodyRemovalUntilWorldUnlocks() {
    config.iceCoverCount = 1;
    ice.update(0f, arena);
    var cover = ice.covers.getFirst();
    Fixture fixture = cover.entity.getComponent(ColliderComponent.class).getFixture();
    Body coverBody = fixture.getBody();
    for (int hit = 0; hit < 3; hit++) ice.hitByFire(fixture);
    Entity projectile =
        physicalEntity(cover.bounds, BodyType.DynamicBody, true, PhysicsLayer.DEFAULT);
    AtomicInteger callbacks = new AtomicInteger();
    int bodyCount = world.getBodyCount();
    projectile
        .getEvents()
        .addListener(
            "collisionStart",
            (Fixture self, Fixture other) -> {
              if (other != fixture) return;
              callbacks.incrementAndGet();
              assertTrue(world.isLocked());
              assertTrue(ice.hitByFire(other));
              assertTrue(ice.covers.isEmpty());
              assertTrue(coverBody.isActive());
              assertEquals(bodyCount, world.getBodyCount());
              ice.clear(); // Clearing again before the queued disposal must not destroy twice.
            });
    doAnswer(
            invocation -> {
              assertFalse(world.isLocked());
              return invocation.callRealMethod();
            })
        .when(physics)
        .destroyBody(any(Body.class));

    world.step(PhysicsEngine.PHYSICS_TIMESTEP, 6, 2);

    assertEquals(1, callbacks.get());
    verify(physics, never()).destroyBody(coverBody);
    entities.update();
    assertEquals(bodyCount - 1, world.getBodyCount());
    verify(physics, times(1)).destroyBody(coverBody);
  }

  @Test
  void spawningDuringLockedContactIsDeferredAndClearInvalidatesQueuedGeneration() {
    AtomicInteger callbacks = new AtomicInteger();
    Entity trigger = contactTrigger();
    trigger
        .getEvents()
        .addListener(
            "collisionStart",
            (Fixture self, Fixture other) -> {
              callbacks.incrementAndGet();
              assertTrue(world.isLocked());
              ice.update(0f, arena);
              assertTrue(ice.covers.isEmpty());
              ice.clear();
            });
    world.step(PhysicsEngine.PHYSICS_TIMESTEP, 6, 2);
    assertEquals(1, callbacks.get());
    entities.update();
    assertTrue(ice.covers.isEmpty());
    assertEquals(2, world.getBodyCount());
    ice.update(0f, arena);
    assertEquals(config.iceCoverCount, ice.covers.size());
  }

  @Test
  void deferredSpawnUsesLatestArenaAndRunsAfterUnlock() {
    Entity trigger = contactTrigger();
    trigger
        .getEvents()
        .addListener(
            "collisionStart",
            (Fixture self, Fixture other) -> {
              ice.update(0f, arena);
              assertTrue(ice.covers.isEmpty());
            });
    world.step(PhysicsEngine.PHYSICS_TIMESTEP, 6, 2);
    Rectangle movedArena = new Rectangle(40f, 40f, 24f, 20f);
    ice.update(0f, movedArena);
    entities.update();
    assertEquals(config.iceCoverCount, ice.covers.size());
    for (var cover : ice.covers) assertTrue(movedArena.contains(cover.bounds));
  }

  private Entity contactTrigger() {
    Rectangle outsideArena = new Rectangle(-30f, -30f, 1f, 1f);
    physicalEntity(outsideArena, BodyType.StaticBody, false, PhysicsLayer.OBSTACLE);
    return physicalEntity(outsideArena, BodyType.DynamicBody, true, PhysicsLayer.DEFAULT);
  }

  private Entity physicalEntity(Rectangle bounds, BodyType type, boolean sensor, short layer) {
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(
        bounds.width / 2f,
        bounds.height / 2f,
        new Vector2(bounds.width / 2f, bounds.height / 2f),
        0f);
    Entity entity =
        new Entity()
            .addComponent(new PhysicsComponent(physics).setBodyType(type))
            .addComponent(
                new ColliderComponent().setShape(shape).setSensor(sensor).setLayer(layer));
    entity.setPosition(bounds.x, bounds.y);
    entity.setScale(bounds.width, bounds.height);
    entity.create();
    shape.dispose();
    return entity;
  }

  private void breakAllCovers() {
    for (var cover : new ArrayList<>(ice.covers)) breakCover(cover);
  }

  private void breakCover(FinalBossStageTwoIceController.Cover cover) {
    Fixture fixture = cover.entity.getComponent(ColliderComponent.class).getFixture();
    for (int hit = 0; hit < config.iceCoverHits; hit++) assertTrue(ice.hitByFire(fixture));
  }

  private static Rectangle expanded(Rectangle rectangle, float gap) {
    return new Rectangle(
        rectangle.x - gap,
        rectangle.y - gap,
        rectangle.width + gap * 2f,
        rectangle.height + gap * 2f);
  }

  private static Random midpointRandom() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.5f);
    return random;
  }

  private void assertNearbyOnBossSide(Rectangle cover) {
    Vector2 fromPlayer = cover.getCenter(new Vector2()).sub(player.getCenterPosition());
    assertTrue(fromPlayer.len() >= config.iceCoverNearMinDistance - 0.001f);
    assertTrue(fromPlayer.len() <= config.iceCoverNearMaxDistance + 0.001f);
    Vector2 towardBoss = boss.getCenterPosition().sub(player.getCenterPosition()).nor();
    assertTrue(fromPlayer.nor().dot(towardBoss) >= Math.cos(Math.toRadians(50f)) - 0.001f);
  }
}
