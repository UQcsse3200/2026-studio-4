package com.csse3200.game.ai.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ObstacleNavigationTest {
  private Entity mover;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
    mover = new Entity().addComponent(new PhysicsComponent()).addComponent(new ColliderComponent());
    mover
        .getComponent(ColliderComponent.class)
        .setAsBox(new Vector2(0.9f, 0.4f), new Vector2(0.5f, 0.2f));
    mover.create();
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.getPhysicsService().getPhysics().dispose();
  }

  @Test
  void openGroundAndWalkableSensorsAllowDirectMovement() {
    Entity sensor = wall(1.4f, -1f, 0.2f, 3f);
    sensor.getComponent(ColliderComponent.class).setSensor(true);
    Vector2 destination = new Vector2(3f, 0f);

    assertTrue(ObstacleNavigation.isClear(mover, destination));
    assertEquals(List.of(destination), ObstacleNavigation.findPath(mover, destination));
  }

  @Test
  void routesAroundWallWithSpaceForTheWholeBody() {
    wall(1.4f, -1f, 0.2f, 3f);
    Vector2 destination = new Vector2(3f, 0f);

    assertFalse(ObstacleNavigation.isClear(mover, destination));
    List<Vector2> route = ObstacleNavigation.findPath(mover, destination);
    assertTrue(route.size() >= 3);
    assertEquals(destination, route.getLast());
    assertRouteClear(route);
  }

  @Test
  void narrowDoorwayDoesNotDependOnGridAlignment() {
    mover.setPosition(-2f, 0f);
    wall(0f, -3f, 0.2f, 3f);
    wall(0f, 0.6f, 0.2f, 3f);
    Vector2 destination = new Vector2(2f, 0.7f);

    List<Vector2> route = ObstacleNavigation.findPath(mover, destination);

    assertFalse(route.isEmpty(), "The 0.4-high collider fits through the 0.6-high doorway");
    assertRouteClear(route);
    assertEquals(destination, route.getLast());
  }

  @Test
  void bodyCannotFitThroughGapEvenWhenItsOriginHasClearSight() {
    wall(1.4f, -10f, 0.2f, 9.9f);
    wall(1.4f, 0.2f, 0.2f, 10f);

    assertFalse(ObstacleNavigation.isClear(mover, new Vector2(3f, 0f)));
    assertTrue(ObstacleNavigation.findPath(mover, new Vector2(3f, 0f)).isEmpty());
  }

  @Test
  void connectedCornerWallsCannotBeCutDiagonally() {
    wall(1.4f, -1f, 0.2f, 2f);
    wall(1.4f, 0.8f, 1.6f, 0.2f);
    Vector2 destination = new Vector2(3.4f, 1.2f);

    List<Vector2> route = ObstacleNavigation.findPath(mover, destination);

    assertFalse(route.isEmpty());
    assertRouteClear(route);
    assertEquals(destination, route.getLast());
  }

  @Test
  void removingBarrierMakesDestinationReachableAgain() {
    Entity barrier = wall(1.4f, -10f, 0.2f, 20f);
    Vector2 destination = new Vector2(3f, 0f);
    assertTrue(ObstacleNavigation.findPath(mover, destination).isEmpty());

    barrier.dispose();

    assertEquals(List.of(destination), ObstacleNavigation.findPath(mover, destination));
  }

  @Test
  void usesColliderOffsetInsteadOfSpriteCenter() {
    mover.setScale(5f, 5f);
    wall(-1f, 0.6f, 6f, 0.2f);

    assertTrue(ObstacleNavigation.isClear(mover, new Vector2(3f, 0f)));
  }

  private void assertRouteClear(List<Vector2> route) {
    for (Vector2 waypoint : route) {
      assertTrue(ObstacleNavigation.isClear(mover, waypoint), "Blocked segment to " + waypoint);
      mover.setPosition(waypoint);
    }
  }

  private Entity wall(float x, float y, float width, float height) {
    Entity wall = ObstacleFactory.createWall(width, height);
    wall.setPosition(x, y);
    wall.create();
    return wall;
  }
}
