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
import com.badlogic.gdx.physics.box2d.PolygonShape;
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

  private Rectangle footprint(Vector2 centre, Vector2 halfSize) {
    return new Rectangle(
        centre.x - halfSize.x, centre.y - halfSize.y, halfSize.x * 2f, halfSize.y * 2f);
  }
}
