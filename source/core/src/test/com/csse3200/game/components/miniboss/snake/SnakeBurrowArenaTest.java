package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SnakeBurrowArenaTest {
  private World world;

  @AfterEach
  void disposeWorld() {
    if (world != null) {
      world.dispose();
    }
  }

  @Test
  void constrainsEntireFootprintWithoutChangingInputs() {
    Rectangle bounds = new Rectangle(2f, 3f, 8f, 6f);
    SnakeBurrowArena arena = new SnakeBurrowArena(bounds);
    Vector2 desired = new Vector2(-4f, 30f);
    Vector2 halfSize = new Vector2(1f, 1.5f);
    bounds.set(50f, 50f, 1f, 1f);

    Vector2 result = arena.constrain(desired, halfSize);

    assertEquals(new Vector2(3f, 7.5f), result);
    assertEquals(new Vector2(-4f, 30f), desired);
    assertEquals(new Vector2(1f, 1.5f), halfSize);
  }

  @Test
  void findsConstrainedPositionWhenPhysicsIsUnavailable() {
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 6f));

    Vector2 result =
        arena.findClearCentre(new Vector2(20f, -2f), new Vector2(0.5f, 1f)).orElseThrow();

    assertEquals(new Vector2(7.5f, 1f), result);
  }

  @Test
  void unrestrictedArenaReturnsAnIndependentPosition() {
    SnakeBurrowArena arena = new SnakeBurrowArena(null);
    Vector2 desired = new Vector2(-10f, 40f);

    Vector2 result = arena.findClearCentre(desired, new Vector2(1f, 1f)).orElseThrow();

    assertEquals(desired, result);
    assertNotSame(desired, result);
  }

  @Test
  void refusesFootprintLargerThanRoom() {
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(2f, 3f, 1f, 8f));
    Vector2 halfSize = new Vector2(1f, 1f);

    assertEquals(new Vector2(2.5f, 4f), arena.constrain(new Vector2(), halfSize));
    assertTrue(arena.findClearCentre(new Vector2(), halfSize).isEmpty());
  }

  @Test
  void movesWholeEmergenceFootprintAwayFromStaticWall() {
    createWorld();
    Rectangle wall = new Rectangle(3.5f, 3f, 0.5f, 2f);
    createBody(wall, BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));
    Vector2 desired = new Vector2(4.2f, 4f);
    Vector2 halfSize = new Vector2(0.6f, 0.6f);

    Vector2 result = arena.findClearCentre(desired, halfSize).orElseThrow();

    assertFalse(wall.overlaps(footprint(result, halfSize)));
    assertTrue(result.dst(desired) <= 2.81f);
    assertTrue(new Rectangle(0f, 0f, 8f, 8f).contains(footprint(result, halfSize)));
    assertEquals(new Vector2(4.2f, 4f), desired);
  }

  @Test
  void returnsEmptyWhenEveryCandidateIsBlocked() {
    createWorld();
    createBody(new Rectangle(0f, 0f, 8f, 8f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));

    assertTrue(arena.findClearCentre(new Vector2(4f, 4f), new Vector2(0.5f, 0.5f)).isEmpty());
  }

  @Test
  void doesNotSearchBeyondNearbyAreaToEscapeAThickWall() {
    createWorld();
    createBody(new Rectangle(-4f, -4f, 8f, 8f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(null);

    assertTrue(arena.findClearCentre(new Vector2(), new Vector2(0.5f, 0.5f)).isEmpty());
  }

  @Test
  void dynamicPlayerAndStaticSensorsDoNotBlockEmergence() {
    createWorld();
    Rectangle occupied = new Rectangle(3f, 3f, 2f, 2f);
    createBody(occupied, BodyType.DynamicBody, false);
    createBody(occupied, BodyType.StaticBody, true);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));
    Vector2 desired = new Vector2(4f, 4f);

    assertEquals(desired, arena.findClearCentre(desired, new Vector2(0.5f, 0.5f)).orElseThrow());
  }

  @Test
  void clearEndpointsCannotTunnelThroughAThinWall() {
    createWorld();
    createBody(new Rectangle(3.9f, 0f, 0.1f, 8f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));
    Vector2 from = new Vector2(2f, 4f);
    Vector2 to = new Vector2(6f, 4f);
    Vector2 halfSize = new Vector2(0.4f, 0.4f);

    assertTrue(arena.isPathClear(from, from, halfSize));
    assertTrue(arena.isPathClear(to, to, halfSize));
    assertFalse(arena.isPathClear(from, to, halfSize));
    assertEquals(from, arena.constrainMovement(from, to, halfSize));
  }

  @Test
  void diagonalPathIgnoresWallsOutsideTheActualSweptFootprint() {
    createWorld();
    createBody(new Rectangle(2f, 5f, 1f, 1f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(null);
    Vector2 from = new Vector2(2f, 2f);
    Vector2 to = new Vector2(6f, 6f);
    Vector2 halfSize = new Vector2(0.4f, 0.4f);

    assertTrue(arena.isPathClear(from, to, halfSize));
    assertEquals(to, arena.constrainMovement(from, to, halfSize));
    assertEquals(to, arena.findReachableCentre(from, to, halfSize).orElseThrow());

    createBody(new Rectangle(3.95f, 3.95f, 0.1f, 0.1f), BodyType.StaticBody, false);
    assertFalse(arena.isPathClear(from, to, halfSize));
  }

  @Test
  void nearbyWallBroadphasePaddingDoesNotBlockClearMovement() {
    createWorld();
    createBody(new Rectangle(4f, 0f, 0.1f, 8f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(null);
    Vector2 from = new Vector2(3.55f, 1f);
    Vector2 to = new Vector2(3.55f, 7f);
    Vector2 halfSize = new Vector2(0.4f, 0.4f);

    assertEquals(from, arena.findClearCentre(from, halfSize).orElseThrow());
    assertTrue(arena.isPathClear(from, to, halfSize));
    assertEquals(to, arena.constrainMovement(from, to, halfSize));
    assertFalse(arena.isPathClear(new Vector2(3.65f, 1f), new Vector2(3.65f, 7f), halfSize));
  }

  @Test
  void rotatedPolygonUsesItsEdgesInsteadOfItsBoundingRectangle() {
    createWorld();
    PolygonShape diamond = new PolygonShape();
    diamond.setAsBox(1f, 1f);
    createStaticShape(diamond, new Vector2(4f, 4f), (float) Math.PI / 4f);
    SnakeBurrowArena arena = new SnakeBurrowArena(null);
    Vector2 clear = new Vector2(2.8f, 5.2f);
    Vector2 halfSize = new Vector2(0.1f, 0.1f);

    assertTrue(arena.isPathClear(clear, clear, halfSize));
    assertEquals(clear, arena.findClearCentre(clear, halfSize).orElseThrow());
    assertFalse(arena.isPathClear(new Vector2(2f, 4f), new Vector2(6f, 4f), halfSize));
  }

  @Test
  void circularObstaclesKeepRoundedCornersAndBlockACompleteCrossing() {
    createWorld();
    CircleShape circle = new CircleShape();
    circle.setRadius(0.5f);
    createStaticShape(circle, new Vector2(4f, 4f), 0f);
    SnakeBurrowArena arena = new SnakeBurrowArena(null);
    Vector2 from = new Vector2(2f, 2f);
    Vector2 halfSize = new Vector2(0.3f, 0.3f);

    assertTrue(arena.isPathClear(from, new Vector2(3.3f, 3.3f), halfSize));
    assertFalse(arena.isPathClear(from, new Vector2(3.4f, 3.4f), halfSize));
    assertFalse(arena.isPathClear(from, new Vector2(6f, 6f), halfSize));
    assertFalse(arena.isPathClear(new Vector2(4f, 4f), new Vector2(4f, 4f), halfSize));
  }

  @Test
  void chainFixturesCheckEachEdgeWithoutFillingTheirInterior() {
    createWorld();
    ChainShape chain = new ChainShape();
    chain.createChain(
        new Vector2[] {new Vector2(4f, 1f), new Vector2(4f, 4f), new Vector2(7f, 4f)});
    createStaticShape(chain, new Vector2(), 0f);
    SnakeBurrowArena arena = new SnakeBurrowArena(null);
    Vector2 halfSize = new Vector2(0.2f, 0.2f);

    assertTrue(arena.isPathClear(new Vector2(5f, 1f), new Vector2(6f, 2f), halfSize));
    assertFalse(arena.isPathClear(new Vector2(2f, 2f), new Vector2(6f, 2f), halfSize));
  }

  @Test
  void edgeFixturesBlockCrossingsButPermitParallelMovement() {
    createWorld();
    EdgeShape edge = new EdgeShape();
    edge.set(new Vector2(4f, 1f), new Vector2(4f, 7f));
    createStaticShape(edge, new Vector2(), 0f);
    SnakeBurrowArena arena = new SnakeBurrowArena(null);
    Vector2 halfSize = new Vector2(0.2f, 0.2f);

    assertTrue(arena.isPathClear(new Vector2(3.5f, 1f), new Vector2(3.5f, 7f), halfSize));
    assertFalse(arena.isPathClear(new Vector2(2f, 4f), new Vector2(6f, 4f), halfSize));
  }

  @Test
  void openMovementPreservesItsInputVectors() {
    createWorld();
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));
    Vector2 from = new Vector2(2f, 2f);
    Vector2 desired = new Vector2(6f, 6f);
    Vector2 halfSize = new Vector2(0.5f, 0.5f);

    Vector2 result = arena.constrainMovement(from, desired, halfSize);

    assertEquals(new Vector2(6f, 6f), result);
    assertNotSame(desired, result);
    assertNotSame(from, result);
    assertEquals(new Vector2(2f, 2f), from);
    assertEquals(new Vector2(6f, 6f), desired);
    assertEquals(new Vector2(0.5f, 0.5f), halfSize);
  }

  @Test
  void diagonalMovementSlidesAlongAVerticalWall() {
    createWorld();
    createBody(new Rectangle(3.9f, 0f, 0.2f, 8f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));

    Vector2 result =
        arena.constrainMovement(new Vector2(2f, 2f), new Vector2(6f, 6f), new Vector2(0.4f, 0.4f));

    assertEquals(new Vector2(2f, 6f), result);
  }

  @Test
  void diagonalMovementSlidesAlongAHorizontalWall() {
    createWorld();
    createBody(new Rectangle(0f, 3.9f, 8f, 0.2f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));

    Vector2 result =
        arena.constrainMovement(new Vector2(2f, 2f), new Vector2(6f, 6f), new Vector2(0.4f, 0.4f));

    assertEquals(new Vector2(6f, 2f), result);
  }

  @Test
  void blockedCornerRetainsAnIndependentCopyOfTheCurrentPosition() {
    createWorld();
    createBody(new Rectangle(3.9f, 0f, 0.2f, 8f), BodyType.StaticBody, false);
    createBody(new Rectangle(0f, 3.9f, 8f, 0.2f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));
    Vector2 from = new Vector2(2f, 2f);

    Vector2 result = arena.constrainMovement(from, new Vector2(6f, 6f), new Vector2(0.4f, 0.4f));

    assertEquals(new Vector2(2f, 2f), result);
    assertNotSame(from, result);
  }

  @Test
  void reachableSearchKeepsTheEmergenceOnTheCurrentSideOfAWall() {
    createWorld();
    createBody(new Rectangle(3.9f, 0f, 0.2f, 8f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));
    Vector2 from = new Vector2(2f, 4f);
    Vector2 desired = new Vector2(5.5f, 4f);
    Vector2 halfSize = new Vector2(0.4f, 0.4f);

    assertEquals(desired, arena.findClearCentre(desired, halfSize).orElseThrow());
    Vector2 result = arena.findReachableCentre(from, desired, halfSize).orElseThrow();

    assertTrue(result.x + halfSize.x < 3.9f);
    assertTrue(result.dst(desired) <= 2.81f);
    assertTrue(arena.isPathClear(from, result, halfSize));
    assertEquals(new Vector2(2f, 4f), from);
    assertEquals(new Vector2(5.5f, 4f), desired);
  }

  @Test
  void reachableSearchReturnsEmptyWhenAllNearbySpaceIsAcrossAWall() {
    createWorld();
    createBody(new Rectangle(2.9f, 0f, 0.2f, 8f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));

    assertTrue(
        arena
            .findReachableCentre(new Vector2(1f, 4f), new Vector2(7f, 4f), new Vector2(0.4f, 0.4f))
            .isEmpty());
  }

  @Test
  void dynamicActorsAndStaticSensorsDoNotBlockTheApproach() {
    createWorld();
    Rectangle occupied = new Rectangle(3f, 3f, 2f, 2f);
    createBody(occupied, BodyType.DynamicBody, false);
    createBody(occupied, BodyType.StaticBody, true);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));
    Vector2 from = new Vector2(2f, 4f);
    Vector2 desired = new Vector2(6f, 4f);
    Vector2 halfSize = new Vector2(0.4f, 0.4f);

    assertTrue(arena.isPathClear(from, desired, halfSize));
    assertEquals(desired, arena.constrainMovement(from, desired, halfSize));
    assertEquals(desired, arena.findReachableCentre(from, desired, halfSize).orElseThrow());
  }

  @Test
  void pathRequiresBothEntireFootprintsInsideTheRoom() {
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));
    Vector2 from = new Vector2(4f, 4f);
    Vector2 halfSize = new Vector2(0.5f, 0.5f);

    for (Vector2 outside :
        new Vector2[] {
          new Vector2(0.25f, 4f),
          new Vector2(7.75f, 4f),
          new Vector2(4f, 0.25f),
          new Vector2(4f, 7.75f)
        }) {
      assertFalse(arena.isPathClear(from, outside, halfSize));
      assertFalse(arena.isPathClear(outside, from, halfSize));
    }
    assertTrue(arena.isPathClear(from, new Vector2(0.5f, 7.5f), halfSize));
    assertEquals(
        new Vector2(0.5f, 7.5f), arena.constrainMovement(from, new Vector2(-2f, 10f), halfSize));
  }

  @Test
  void startRecoveryMovesAnOverlappingFootprintAwayFromItsOwnSideOfAWall() {
    createWorld();
    createBody(new Rectangle(4f, 0f, 0.1f, 8f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));
    Vector2 current = new Vector2(3.95f, 4f);
    Vector2 halfSize = new Vector2(0.5f, 0.5f);

    assertFalse(arena.isPathClear(current, current, halfSize));
    Vector2 result = arena.findClearStartCentre(current, halfSize).orElseThrow();

    assertTrue(result.x + halfSize.x < 4f);
    assertTrue(arena.isPathClear(result, result, halfSize));
    assertTrue(SnakePoisonCollision.wallFraction(current, result, 0f, null) > 1f);
    assertEquals(new Vector2(3.95f, 4f), current);
    assertEquals(new Vector2(0.5f, 0.5f), halfSize);
  }

  @Test
  void startRecoveryCannotTeleportACentreOutOfAWall() {
    createWorld();
    createBody(new Rectangle(4f, 0f, 0.2f, 8f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));

    assertTrue(
        arena.findClearStartCentre(new Vector2(4.1f, 4f), new Vector2(0.5f, 0.5f)).isEmpty());
  }

  @Test
  void startRecoveryCannotClampAnOutsideCentreAcrossABoundaryWall() {
    createWorld();
    createBody(new Rectangle(0f, 0f, 0.2f, 8f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));

    assertTrue(arena.findClearStartCentre(new Vector2(-1f, 4f), new Vector2(0.5f, 0.5f)).isEmpty());
  }

  @Test
  void startRecoveryPreservesAnAlreadyClearCentre() {
    createWorld();
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 8f, 8f));
    Vector2 current = new Vector2(4f, 4f);

    Vector2 result = arena.findClearStartCentre(current, new Vector2(0.5f, 0.5f)).orElseThrow();

    assertEquals(current, result);
    assertNotSame(current, result);
  }

  @Test
  void configuredSnakeSpawnCanRecoverBetweenAdjacentRoomRecesses() {
    createWorld();
    // dungeonTwoEnd tiles (27,29), (27,30), (28,30), (24,33) and (25,33).
    // These collider bounds follow the half-unit tiles and top-face wall placement.
    createBody(new Rectangle(13.625f, 14.5f, 0.25f, 0.25f), BodyType.StaticBody, false);
    createBody(new Rectangle(13.625f, 15f, 0.25f, 0.25f), BodyType.StaticBody, false);
    createBody(new Rectangle(14.125f, 15f, 0.25f, 0.25f), BodyType.StaticBody, false);
    createBody(new Rectangle(12.125f, 16f, 0.25f, 1f), BodyType.StaticBody, false);
    createBody(new Rectangle(12.625f, 16f, 0.25f, 1f), BodyType.StaticBody, false);
    SnakeBurrowArena arena = new SnakeBurrowArena(new Rectangle(0f, 0f, 31f, 26f));
    // The 2-by-2 Snake is centred on configured spawn tile (26,31).
    Vector2 current = new Vector2(13.25f, 15.75f);
    Vector2 halfSize = new Vector2(1f, 1f);

    assertFalse(arena.isPathClear(current, current, halfSize));
    Vector2 result = arena.findClearStartCentre(current, halfSize).orElseThrow();

    assertTrue(arena.isPathClear(result, result, halfSize));
    assertTrue(SnakePoisonCollision.wallFraction(current, result, 0f, null) > 1f);
    assertTrue(result.dst(current) <= 2.81f);
  }

  private void createWorld() {
    PhysicsService service = new PhysicsService();
    ServiceLocator.registerPhysicsService(service);
    world = service.getPhysics().getWorld();
  }

  private void createBody(Rectangle area, BodyType type, boolean sensor) {
    BodyDef definition = new BodyDef();
    definition.type = type;
    definition.position.set(area.x + area.width / 2f, area.y + area.height / 2f);
    Body body = world.createBody(definition);
    PolygonShape shape = new PolygonShape();
    try {
      shape.setAsBox(area.width / 2f, area.height / 2f);
      body.createFixture(shape, 1f).setSensor(sensor);
    } finally {
      shape.dispose();
    }
  }

  private void createStaticShape(Shape shape, Vector2 position, float angle) {
    BodyDef definition = new BodyDef();
    definition.type = BodyType.StaticBody;
    definition.position.set(position);
    definition.angle = angle;
    try {
      world.createBody(definition).createFixture(shape, 1f);
    } finally {
      shape.dispose();
    }
  }

  private Rectangle footprint(Vector2 centre, Vector2 halfSize) {
    return new Rectangle(
        centre.x - halfSize.x, centre.y - halfSize.y, halfSize.x * 2f, halfSize.y * 2f);
  }
}
