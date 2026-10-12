package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SnakeBurrowNavigatorTest {
  private static final Rectangle ROOM = new Rectangle(0f, 0f, 16f, 12f);
  private static final Vector2 HALF_SIZE = new Vector2(0.9f, 0.4f);
  private World world;
  private SnakeBurrowArena arena;
  private SnakeBurrowNavigator navigator;

  @BeforeEach
  void createWorld() {
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    arena = new SnakeBurrowArena(ROOM);
    navigator = new SnakeBurrowNavigator(arena, ROOM);
  }

  @AfterEach
  void disposeWorld() {
    world.dispose();
  }

  @Test
  void clearApproachReturnsIndependentDestinationWithoutChangingInputs() {
    Vector2 start = new Vector2(2f, 2f);
    Vector2 goal = new Vector2(12f, 9f);

    Vector2 next = navigator.nextWaypoint(start, goal, HALF_SIZE, 0.016f);

    assertEquals(goal, next);
    assertNotSame(goal, next);
    assertEquals(new Vector2(2f, 2f), start);
    assertEquals(new Vector2(12f, 9f), goal);
    assertEquals(new Vector2(0.9f, 0.4f), HALF_SIZE);
    assertFalse(navigator.isDetouring());
  }

  @Test
  void reachesPlayerAroundLongHorizontalWall() {
    wall(4f, 5f, 8f, 0.1f);
    Vector2 start = new Vector2(8f, 3f);
    Vector2 goal = new Vector2(8f, 8f);

    Vector2 next = navigator.nextWaypoint(start, goal, HALF_SIZE, 0.016f);

    assertTrue(navigator.isDetouring());
    assertTrue(Math.abs(next.x - start.x) > 1f);
    followRoute(start, goal, navigator);
  }

  @Test
  void escapesUShapedWallByFirstMovingAwayFromPlayer() {
    wall(4f, 3f, 0.1f, 6f);
    wall(4f, 3f, 8f, 0.1f);
    wall(11.9f, 3f, 0.1f, 6f);
    Vector2 start = new Vector2(8f, 5f);
    Vector2 goal = new Vector2(8f, 1.5f);

    Vector2 next = navigator.nextWaypoint(start, goal, HALF_SIZE, 0.016f);

    assertTrue(next.y > start.y);
    followRoute(start, goal, navigator);
  }

  @Test
  void usesActualWideFootprintWhenNavigatingPastThinWalls() {
    wall(7f, 0f, 0.05f, 5f);
    wall(7f, 6.1f, 0.05f, 5.9f);

    followRoute(new Vector2(3f, 5.5f), new Vector2(12f, 5.5f), navigator);
  }

  @Test
  void retargetsWhenPlayerMovesToOtherSideOfObstacle() {
    wall(4f, 5f, 8f, 0.1f);
    Vector2 start = new Vector2(8f, 3f);
    navigator.nextWaypoint(start, new Vector2(13.5f, 8f), HALF_SIZE, 0.016f);

    followRoute(start, new Vector2(2f, 8f), navigator);
  }

  @Test
  void invalidatesCachedApproachWhenAnotherWallAppears() {
    wall(4f, 5f, 8f, 0.1f);
    Vector2 start = new Vector2(8f, 3f);
    Vector2 goal = new Vector2(8f, 8f);
    Vector2 oldWaypoint = navigator.nextWaypoint(start, goal, HALF_SIZE, 0.016f);
    float x = oldWaypoint.x < start.x ? 4.4f : 11.5f;
    wall(x, 0f, 0.1f, 5f);

    assertFalse(arena.isPathClear(start, oldWaypoint, HALF_SIZE));
    followRoute(start, goal, navigator);
  }

  @Test
  void unreachablePlayerDoesNotCauseTeleportAcrossWall() {
    wall(0f, 5f, 16f, 0.05f);
    Vector2 start = new Vector2(8f, 3f);

    for (int frame = 0; frame < 30; frame++) {
      assertEquals(start, navigator.nextWaypoint(start, new Vector2(8f, 8f), HALF_SIZE, 0.016f));
    }
    assertTrue(navigator.isDetouring());
  }

  @Test
  void unavailableDestinationIsCachedUntilRetryAndResetClearsDelay() {
    SnakeBurrowArena blockedArena = mock(SnakeBurrowArena.class);
    when(blockedArena.constrain(any(), any()))
        .thenAnswer(call -> ((Vector2) call.getArgument(0)).cpy());
    when(blockedArena.findClearCentre(any(), any())).thenReturn(Optional.empty());
    SnakeBurrowNavigator blocked = new SnakeBurrowNavigator(blockedArena, ROOM);
    Vector2 start = new Vector2(2f, 2f);
    Vector2 goal = new Vector2(12f, 9f);
    blocked.nextWaypoint(start, goal, HALF_SIZE, 0f);
    clearInvocations(blockedArena);

    blocked.nextWaypoint(start, goal, HALF_SIZE, 0.1f);
    blocked.nextWaypoint(start, goal, HALF_SIZE, 0.1f);
    verify(blockedArena, never()).findClearCentre(any(), any());

    blocked.nextWaypoint(start, goal, HALF_SIZE, 0.3f);
    verify(blockedArena).findClearCentre(any(), any());
    clearInvocations(blockedArena);
    blocked.reset();
    assertFalse(blocked.isDetouring());
    blocked.nextWaypoint(start, goal, HALF_SIZE, 0f);
    verify(blockedArena).findClearCentre(any(), any());
  }

  @Test
  void failedPathSearchWaitsBeforeTryingAgain() {
    wall(0f, 5f, 16f, 0.05f);
    SnakeBurrowArena observedArena = spy(arena);
    SnakeBurrowNavigator observed = new SnakeBurrowNavigator(observedArena, ROOM);
    Vector2 start = new Vector2(8f, 3f);
    Vector2 goal = new Vector2(8f, 8f);
    observed.nextWaypoint(start, goal, HALF_SIZE, 0f);
    clearInvocations(observedArena);

    observed.nextWaypoint(start, goal, HALF_SIZE, 0.1f);
    observed.nextWaypoint(start, goal, HALF_SIZE, 0.1f);
    verify(observedArena, never()).isPathClear(start, start, HALF_SIZE);

    observed.nextWaypoint(start, goal, HALF_SIZE, 0.3f);
    verify(observedArena).isPathClear(start, start, HALF_SIZE);
  }

  @Test
  void diagonalRouteCannotCutThroughTouchingWallCorners() {
    wall(4f, 2f, 4f, 4f);
    wall(8f, 6f, 4f, 4f);

    followRoute(new Vector2(3f, 7f), new Vector2(12.9f, 5f), navigator);
  }

  @Test
  void playerNearWallUsesClearNearbyApproachWithoutUnnecessaryDetour() {
    wall(8f, 0f, 0.1f, 12f);
    Vector2 start = new Vector2(4f, 6f);
    Vector2 goal = new Vector2(7.5f, 6f);

    Vector2 next = navigator.nextWaypoint(start, goal, HALF_SIZE, 0.016f);

    assertTrue(arena.isPathClear(start, next, HALF_SIZE));
    assertFalse(navigator.isDetouring());
    assertTrue(next.dst(goal) < 2f);
  }

  @Test
  void movingPlayerAcrossWallImmediatelyInvalidatesPreviousNearbyGoal() {
    wall(8f, 0f, 0.1f, 12f);
    Vector2 start = new Vector2(4f, 6f);
    navigator.nextWaypoint(start, new Vector2(7.5f, 6f), HALF_SIZE, 0.016f);
    assertFalse(navigator.isDetouring());

    Vector2 next = navigator.nextWaypoint(start, new Vector2(10f, 6f), HALF_SIZE, 0.01f);

    assertTrue(navigator.isDetouring(), "old clear goal must not start a warning across a wall");
    assertEquals(start, next);
  }

  @Test
  void unlimitedArenaStillRoutesAroundObstacleWithinBoundedSearch() {
    arena = new SnakeBurrowArena(null);
    navigator = new SnakeBurrowNavigator(arena, null);
    wall(4f, 5f, 8f, 0.1f);

    followRoute(new Vector2(8f, 3f), new Vector2(8f, 8f), navigator);
  }

  private void followRoute(Vector2 start, Vector2 goal, SnakeBurrowNavigator routing) {
    Vector2 current = start.cpy();
    for (int frame = 0; frame < 400 && current.dst(goal) > 0.01f; frame++) {
      Vector2 waypoint = routing.nextWaypoint(current, goal, HALF_SIZE, 0.05f);
      assertTrue(
          arena.isPathClear(current, waypoint, HALF_SIZE), "waypoint must clear the full body");
      Vector2 movement = waypoint.cpy().sub(current).limit(0.2f);
      current.add(movement);
    }
    assertTrue(current.dst(goal) <= 0.01f, "route should reach player; stopped at " + current);
    assertFalse(routing.isDetouring());
  }

  private Body wall(float x, float y, float width, float height) {
    BodyDef definition = new BodyDef();
    definition.position.set(x + width / 2f, y + height / 2f);
    Body body = world.createBody(definition);
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(width / 2f, height / 2f);
    body.createFixture(shape, 0f);
    shape.dispose();
    return body;
  }
}
