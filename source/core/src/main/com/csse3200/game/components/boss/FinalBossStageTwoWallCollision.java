package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;

/** Sweeps a fireball's circle against solid static fixtures without changing wall state. */
final class FinalBossStageTwoWallCollision {
  private static final float NONE = Float.POSITIVE_INFINITY;
  private static final float EPSILON = 0.000001f;

  record Hit(Fixture fixture, float fraction) {}

  private FinalBossStageTwoWallCollision() {}

  /** Returns the nearest fixture and segment fraction; infinity means no collision. */
  static Hit firstHit(World world, Vector2 start, Vector2 end, float radius) {
    if (world == null) return new Hit(null, NONE);
    Fixture[] nearestFixture = {null};
    float[] nearest = {NONE};
    world.QueryAABB(
        fixture -> {
          if (!fixture.isSensor() && fixture.getBody().getType() == BodyType.StaticBody) {
            float fraction = fixtureFraction(fixture, start, end, radius);
            if (fraction < nearest[0]) {
              nearest[0] = fraction;
              nearestFixture[0] = fixture;
            }
          }
          return nearest[0] != 0f;
        },
        Math.min(start.x, end.x) - radius,
        Math.min(start.y, end.y) - radius,
        Math.max(start.x, end.x) + radius,
        Math.max(start.y, end.y) + radius);
    return new Hit(nearestFixture[0], nearest[0]);
  }

  private static float fixtureFraction(Fixture fixture, Vector2 start, Vector2 end, float radius) {
    if (fixture.testPoint(start)) return 0f;
    Shape shape = fixture.getShape();
    if (shape instanceof CircleShape circle) {
      return circleFraction(
          start,
          end,
          fixture.getBody().getWorldPoint(circle.getPosition()),
          radius + circle.getRadius());
    }
    Vector2[] vertices = vertices(shape);
    float nearest = NONE;
    int edgeCount = shape instanceof PolygonShape ? vertices.length : vertices.length - 1;
    for (int i = 0; i < edgeCount; i++) {
      Vector2 first = fixture.getBody().getWorldPoint(vertices[i]).cpy();
      Vector2 second = fixture.getBody().getWorldPoint(vertices[(i + 1) % vertices.length]).cpy();
      nearest = Math.min(nearest, edgeFraction(start, end, first, second, radius));
    }
    return nearest;
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
        case Edge -> {
          if (i == 0) ((EdgeShape) shape).getVertex1(result[i]);
          else ((EdgeShape) shape).getVertex2(result[i]);
        }
        default -> {
          // Circles are handled directly.
        }
      }
    }
    return result;
  }

  /** A swept circle touches an edge's strip or one of its rounded endpoints. */
  private static float edgeFraction(
      Vector2 start, Vector2 end, Vector2 first, Vector2 second, float radius) {
    float nearest =
        Math.min(
            circleFraction(start, end, first, radius), circleFraction(start, end, second, radius));
    Vector2 tangent = second.cpy().sub(first);
    float length = tangent.len();
    if (length <= EPSILON) return nearest;
    tangent.scl(1f / length);
    Vector2 relative = start.cpy().sub(first);
    Vector2 movement = end.cpy().sub(start);
    float[] interval = {0f, 1f};
    boolean crosses =
        intersectSlab(relative.dot(tangent), movement.dot(tangent), 0f, length, interval)
            && intersectSlab(
                relative.crs(tangent), movement.crs(tangent), -radius, radius, interval);
    return crosses ? Math.min(nearest, interval[0]) : nearest;
  }

  private static boolean intersectSlab(
      float start, float movement, float min, float max, float[] interval) {
    if (Math.abs(movement) < EPSILON) return start >= min && start <= max;
    float first = (min - start) / movement;
    float second = (max - start) / movement;
    interval[0] = Math.max(interval[0], Math.min(first, second));
    interval[1] = Math.min(interval[1], Math.max(first, second));
    return interval[0] <= interval[1];
  }

  private static float circleFraction(Vector2 start, Vector2 end, Vector2 centre, float radius) {
    double offsetX = (double) start.x - centre.x;
    double offsetY = (double) start.y - centre.y;
    double c = offsetX * offsetX + offsetY * offsetY - (double) radius * radius;
    if (c <= 0d) return 0f;
    double moveX = (double) end.x - start.x;
    double moveY = (double) end.y - start.y;
    double a = moveX * moveX + moveY * moveY;
    if (a <= (double) EPSILON * EPSILON) return NONE;
    double b = offsetX * moveX + offsetY * moveY;
    double discriminant = b * b - a * c;
    if (discriminant < 0d) return NONE;
    double fraction = (-b - Math.sqrt(discriminant)) / a;
    return fraction >= 0d && fraction <= 1d ? (float) fraction : NONE;
  }
}
