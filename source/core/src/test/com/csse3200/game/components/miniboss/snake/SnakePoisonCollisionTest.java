package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.MathUtils;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Real-fixture regressions for finite-radius poison shots and their first impact positions. */
@ExtendWith(GameExtension.class)
class SnakePoisonCollisionTest {
  private static final float EPSILON = 0.00001f;
  private World world;

  @BeforeEach
  void createWorld() {
    PhysicsService service = new PhysicsService();
    ServiceLocator.registerPhysicsService(service);
    world = service.getPhysics().getWorld();
  }

  @AfterEach
  void disposeWorld() {
    world.dispose();
  }

  @Test
  void fastShotClipsThinWallEvenWhenItsCentrelineMisses() {
    addBox(new Rectangle(5f, 2f, 0.02f, 1f), BodyType.StaticBody, false);
    Vector2 start = new Vector2(0f, 1.85f);
    Vector2 end = new Vector2(10f, 1.85f);

    // The radius reaches the wall's lower-left corner at x = 4.8, between updates.
    assertEquals(0.48f, SnakePoisonCollision.wallFraction(start, end, 0.25f, null), EPSILON);
    assertEquals(Float.POSITIVE_INFINITY, SnakePoisonCollision.wallFraction(start, end, 0f, null));
  }

  @Test
  void blockedSpawnIsDetectedForBothCentreAndRadiusOverlap() {
    addBox(new Rectangle(2f, 2f, 2f, 2f), BodyType.StaticBody, false);

    assertEquals(
        0f,
        SnakePoisonCollision.wallFraction(new Vector2(3f, 3f), new Vector2(6f, 3f), 0.25f, null));
    assertEquals(
        0f,
        SnakePoisonCollision.wallFraction(
            new Vector2(1.8f, 3f), new Vector2(1.8f, 3f), 0.25f, null));
  }

  @Test
  void circleAndRotatedPolygonUseTheirWorldTransforms() {
    CircleShape circle = new CircleShape();
    circle.setRadius(0.5f);
    circle.setPosition(new Vector2(0f, 2f));
    addFixture(circle, new Vector2(10f, 4f), MathUtils.PI / 2f, BodyType.StaticBody, false);

    PolygonShape polygon = new PolygonShape();
    polygon.setAsBox(2f, 0.5f);
    addFixture(polygon, new Vector2(10f, 10f), MathUtils.PI / 2f, BodyType.StaticBody, false);

    assertEquals(
        0.725f,
        SnakePoisonCollision.wallFraction(new Vector2(0f, 4f), new Vector2(10f, 4f), 0.25f, null),
        EPSILON);
    assertEquals(
        9.25f / 12f,
        SnakePoisonCollision.wallFraction(new Vector2(0f, 11f), new Vector2(12f, 11f), 0.25f, null),
        EPSILON);
  }

  @Test
  void sensorsAndDynamicBodiesDoNotHideTheFirstSolidWall() {
    addBox(new Rectangle(2f, -1f, 1f, 2f), BodyType.StaticBody, true);
    addBox(new Rectangle(4f, -1f, 1f, 2f), BodyType.DynamicBody, false);
    Vector2 start = new Vector2(0f, 0f);
    Vector2 end = new Vector2(10f, 0f);

    assertEquals(
        Float.POSITIVE_INFINITY, SnakePoisonCollision.wallFraction(start, end, 0.25f, null));

    addBox(new Rectangle(8f, -1f, 1f, 2f), BodyType.StaticBody, false);

    assertEquals(0.775f, SnakePoisonCollision.wallFraction(start, end, 0.25f, null), EPSILON);
  }

  @Test
  void arenaStopsTheWholeProjectileAndRejectsInvalidSpawns() {
    Rectangle bounds = new Rectangle(2f, 3f, 8f, 6f);
    Vector2 start = new Vector2(4f, 5f);

    assertEquals(
        0.6875f,
        SnakePoisonCollision.wallFraction(start, new Vector2(12f, 5f), 0.5f, bounds),
        EPSILON);
    assertEquals(
        0.3f, SnakePoisonCollision.wallFraction(start, new Vector2(4f, 0f), 0.5f, bounds), EPSILON);
    assertEquals(0f, SnakePoisonCollision.wallFraction(new Vector2(2.4f, 5f), start, 0.5f, bounds));
    assertEquals(
        0f,
        SnakePoisonCollision.wallFraction(
            new Vector2(0.3f, 1f), new Vector2(0.3f, 2f), 0.5f, new Rectangle(0f, 0f, 0.6f, 4f)));
  }

  @Test
  void circleSweepHandlesStationaryOverlapTangencyAndEndpointHits() {
    Vector2 centre = new Vector2(2f, 0f);

    assertEquals(0f, SnakePoisonCollision.circleFraction(centre, centre, centre, 1f));
    assertEquals(
        Float.POSITIVE_INFINITY,
        SnakePoisonCollision.circleFraction(new Vector2(), new Vector2(), centre, 1f));
    assertEquals(
        1f,
        SnakePoisonCollision.circleFraction(new Vector2(), new Vector2(1f, 0f), centre, 1f),
        EPSILON);
    assertEquals(
        0.5f,
        SnakePoisonCollision.circleFraction(
            new Vector2(), new Vector2(4f, 0f), new Vector2(2f, 1f), 1f),
        EPSILON);
  }

  @Test
  void passingOutsideRoundedCornerDoesNotHitTheExpandedBoundingBox() {
    addBox(new Rectangle(2f, 2f, 2f, 2f), BodyType.StaticBody, false);

    assertEquals(
        Float.POSITIVE_INFINITY,
        SnakePoisonCollision.wallFraction(
            new Vector2(1.78f, 1.78f), new Vector2(1.8f, 1.8f), 0.25f, null));
  }

  @Test
  void edgeAndChainFixturesStopShotsUsingTheirWorldPositions() {
    EdgeShape edge = new EdgeShape();
    edge.set(new Vector2(0f, -1f), new Vector2(0f, 1f));
    addFixture(edge, new Vector2(3f, 0f), 0f, BodyType.StaticBody, false);

    ChainShape chain = new ChainShape();
    chain.createChain(
        new Vector2[] {new Vector2(0f, -1f), new Vector2(0f, 1f), new Vector2(2f, 1f)});
    addFixture(chain, new Vector2(8f, 5f), 0f, BodyType.StaticBody, false);

    assertEquals(
        0.28f,
        SnakePoisonCollision.wallFraction(new Vector2(), new Vector2(10f, 0f), 0.2f, null),
        EPSILON);
    assertEquals(
        0.78f,
        SnakePoisonCollision.wallFraction(new Vector2(0f, 5f), new Vector2(10f, 5f), 0.2f, null),
        EPSILON);
  }

  private void addBox(Rectangle area, BodyType type, boolean sensor) {
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(area.width / 2f, area.height / 2f);
    addFixture(
        shape, new Vector2(area.x + area.width / 2f, area.y + area.height / 2f), 0f, type, sensor);
  }

  private void addFixture(
      Shape shape, Vector2 position, float angle, BodyType type, boolean sensor) {
    try {
      BodyDef definition = new BodyDef();
      definition.type = type;
      definition.position.set(position);
      definition.angle = angle;
      Body body = world.createBody(definition);
      body.createFixture(shape, 1f).setSensor(sensor);
    } finally {
      shape.dispose();
    }
  }
}
