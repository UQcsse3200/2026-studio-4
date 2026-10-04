package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.ServiceLocator;

/** Swept circular projectiles: results are segment fractions, infinity means no collision. */
final class SnakePoisonCollision {
  private static final float NONE = Float.POSITIVE_INFINITY;
  private static final float EPSILON = 0.000001f;

  private SnakePoisonCollision() {}

  static float wallFraction(Vector2 start, Vector2 end, float radius, Rectangle bounds) {
    float[] closest = {boundaryFraction(start, end, radius, bounds)};
    PhysicsService physics = ServiceLocator.getPhysicsService();
    if (physics == null) {
      return closest[0];
    }
    physics
        .getPhysics()
        .getWorld()
        .QueryAABB(
            fixture -> {
              if (!fixture.isSensor() && fixture.getBody().getType() == BodyType.StaticBody) {
                closest[0] = Math.min(closest[0], fixtureFraction(fixture, start, end, radius));
              }
              return closest[0] != 0f;
            },
            Math.min(start.x, end.x) - radius,
            Math.min(start.y, end.y) - radius,
            Math.max(start.x, end.x) + radius,
            Math.max(start.y, end.y) + radius);
    return closest[0];
  }

  static float circleFraction(Vector2 start, Vector2 end, Vector2 centre, float radius) {
    Vector2 offset = start.cpy().sub(centre);
    float c = offset.len2() - radius * radius;
    if (c <= 0f) {
      return 0f;
    }
    Vector2 movement = end.cpy().sub(start);
    float a = movement.len2();
    if (a <= EPSILON) {
      return NONE;
    }
    float b = offset.dot(movement);
    float discriminant = b * b - a * c;
    if (discriminant < 0f) {
      return NONE;
    }
    float fraction = (-b - (float) Math.sqrt(discriminant)) / a;
    return fraction >= 0f && fraction <= 1f ? fraction : NONE;
  }

  private static float boundaryFraction(
      Vector2 start, Vector2 end, float radius, Rectangle bounds) {
    if (bounds == null) {
      return NONE;
    }
    float minX = bounds.x + radius;
    float maxX = bounds.x + bounds.width - radius;
    float minY = bounds.y + radius;
    float maxY = bounds.y + bounds.height - radius;
    if (start.x < minX || start.x > maxX || start.y < minY || start.y > maxY) {
      return 0f;
    }
    return Math.min(
        exitFraction(start.x, end.x, minX, maxX), exitFraction(start.y, end.y, minY, maxY));
  }

  private static float exitFraction(float start, float end, float min, float max) {
    if (end < min) {
      return (min - start) / (end - start);
    }
    if (end > max) {
      return (max - start) / (end - start);
    }
    return NONE;
  }

  private static float fixtureFraction(Fixture fixture, Vector2 start, Vector2 end, float radius) {
    if (fixture.testPoint(start)) {
      return 0f;
    }
    Shape shape = fixture.getShape();
    if (shape instanceof CircleShape circle) {
      return circleFraction(
          start,
          end,
          fixture.getBody().getWorldPoint(circle.getPosition()),
          radius + circle.getRadius());
    }
    Vector2[] vertices = vertices(shape);
    float closest = NONE;
    int edgeCount = shape instanceof PolygonShape ? vertices.length : vertices.length - 1;
    for (int i = 0; i < edgeCount; i++) {
      Vector2 first = fixture.getBody().getWorldPoint(vertices[i]).cpy();
      Vector2 second = fixture.getBody().getWorldPoint(vertices[(i + 1) % vertices.length]).cpy();
      closest = Math.min(closest, edgeFraction(start, end, first, second, radius));
    }
    return closest;
  }

  private static Vector2[] vertices(Shape shape) {
    int count =
        switch (shape.getType()) {
          case Polygon -> ((PolygonShape) shape).getVertexCount();
          case Chain -> ((ChainShape) shape).getVertexCount();
          case Edge -> 2;
          default -> 0;
        };
    Vector2[] result = new Vector2[count];
    for (int i = 0; i < count; i++) {
      result[i] = new Vector2();
      switch (shape.getType()) {
        case Polygon -> ((PolygonShape) shape).getVertex(i, result[i]);
        case Chain -> ((ChainShape) shape).getVertex(i, result[i]);
        case Edge -> edgeVertex((EdgeShape) shape, i, result[i]);
        default -> {
          /* Circles are handled directly. */
        }
      }
    }
    return result;
  }

  private static void edgeVertex(EdgeShape shape, int index, Vector2 output) {
    if (index == 0) {
      shape.getVertex1(output);
    } else {
      shape.getVertex2(output);
    }
  }

  private static float edgeFraction(
      Vector2 start, Vector2 end, Vector2 first, Vector2 second, float radius) {
    float closest =
        Math.min(
            circleFraction(start, end, first, radius), circleFraction(start, end, second, radius));
    Vector2 tangent = second.cpy().sub(first);
    float length = tangent.len();
    if (length <= EPSILON) {
      return closest;
    }
    tangent.scl(1f / length);
    Vector2 relative = start.cpy().sub(first);
    Vector2 movement = end.cpy().sub(start);
    float along = relative.dot(tangent);
    float normal = relative.crs(tangent);
    float[] interval = {0f, 1f};
    boolean crosses =
        intersectSlab(along, movement.dot(tangent), 0f, length, interval)
            && intersectSlab(normal, movement.crs(tangent), -radius, radius, interval);
    return crosses ? Math.min(closest, interval[0]) : closest;
  }

  private static boolean intersectSlab(
      float start, float movement, float min, float max, float[] interval) {
    if (Math.abs(movement) < EPSILON) {
      return start >= min && start <= max;
    }
    float first = (min - start) / movement;
    float second = (max - start) / movement;
    interval[0] = Math.max(interval[0], Math.min(first, second));
    interval[1] = Math.min(interval[1], Math.max(first, second));
    return interval[0] <= interval[1];
  }
}
