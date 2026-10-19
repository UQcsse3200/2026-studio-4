package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;

/** Narrow-phase collision for an axis-aligned burrow footprint moving along a straight segment. */
final class SnakeBurrowCollision {
  private SnakeBurrowCollision() {}

  static boolean intersects(Fixture fixture, Vector2 from, Vector2 to, Vector2 halfSize) {
    Shape shape = fixture.getShape();
    if (shape instanceof CircleShape circle) {
      Vector2 centre = fixture.getBody().getWorldPoint(circle.getPosition()).cpy();
      return intersectsCircle(from, to, halfSize, centre, circle.getRadius());
    }
    Vector2[] vertices = worldVertices(fixture);
    if (shape instanceof PolygonShape) {
      return intersectsConvex(from, to, halfSize, vertices);
    }
    for (int i = 1; i < vertices.length; i++) {
      if (intersectsConvex(from, to, halfSize, new Vector2[] {vertices[i - 1], vertices[i]})) {
        return true;
      }
    }
    return false;
  }

  private static Vector2[] worldVertices(Fixture fixture) {
    Shape shape = fixture.getShape();
    int count =
        switch (shape.getType()) {
          case Polygon -> ((PolygonShape) shape).getVertexCount();
          case Chain -> ((ChainShape) shape).getVertexCount();
          case Edge -> 2;
          default -> 0;
        };
    Vector2[] vertices = new Vector2[count];
    for (int i = 0; i < count; i++) {
      Vector2 vertex = new Vector2();
      readVertex(shape, i, vertex);
      vertices[i] = fixture.getBody().getWorldPoint(vertex).cpy();
    }
    return vertices;
  }

  private static void readVertex(Shape shape, int index, Vector2 output) {
    switch (shape.getType()) {
      case Polygon -> ((PolygonShape) shape).getVertex(index, output);
      case Chain -> ((ChainShape) shape).getVertex(index, output);
      case Edge -> {
        EdgeShape edge = (EdgeShape) shape;
        if (index == 0) {
          edge.getVertex1(output);
        } else {
          edge.getVertex2(output);
        }
      }
      default -> {
        // Circles use their centre and radius directly.
      }
    }
  }

  private static boolean intersectsConvex(
      Vector2 from, Vector2 to, Vector2 halfSize, Vector2[] vertices) {
    // The swept box is convex, with horizontal, vertical and travel-parallel edges.
    // SAT also needs the fixture's edge normals; checking this complete set is exact.
    if (separated(from, to, halfSize, vertices, 1f, 0f)
        || separated(from, to, halfSize, vertices, 0f, 1f)
        || separated(from, to, halfSize, vertices, to.y - from.y, from.x - to.x)) {
      return false;
    }
    for (int i = 0; i < vertices.length; i++) {
      Vector2 first = vertices[i];
      Vector2 second = vertices[(i + 1) % vertices.length];
      if (separated(from, to, halfSize, vertices, second.y - first.y, first.x - second.x)) {
        return false;
      }
    }
    return true;
  }

  private static boolean separated(
      Vector2 from, Vector2 to, Vector2 halfSize, Vector2[] vertices, float axisX, float axisY) {
    float wallMin = Float.POSITIVE_INFINITY;
    float wallMax = Float.NEGATIVE_INFINITY;
    for (Vector2 vertex : vertices) {
      float projection = vertex.x * axisX + vertex.y * axisY;
      wallMin = Math.min(wallMin, projection);
      wallMax = Math.max(wallMax, projection);
    }
    float start = from.x * axisX + from.y * axisY;
    float end = to.x * axisX + to.y * axisY;
    float radius = halfSize.x * Math.abs(axisX) + halfSize.y * Math.abs(axisY);
    return Math.max(start, end) + radius < wallMin || Math.min(start, end) - radius > wallMax;
  }

  private static boolean intersectsCircle(
      Vector2 from, Vector2 to, Vector2 halfSize, Vector2 centre, float radius) {
    // Expanding the box by a circle produces two rectangles and four rounded corners.
    // Test each part continuously so a long step cannot skip a circular obstacle.
    if (intersectsRectangle(from, to, centre, halfSize.x + radius, halfSize.y)
        || intersectsRectangle(from, to, centre, halfSize.x, halfSize.y + radius)) {
      return true;
    }
    for (int x : new int[] {-1, 1}) {
      for (int y : new int[] {-1, 1}) {
        Vector2 corner = new Vector2(centre.x + x * halfSize.x, centre.y + y * halfSize.y);
        if (distanceToSegmentSquared(from, to, corner) <= radius * radius) {
          return true;
        }
      }
    }
    return false;
  }

  private static float distanceToSegmentSquared(Vector2 from, Vector2 to, Vector2 point) {
    Vector2 movement = to.cpy().sub(from);
    float lengthSquared = movement.len2();
    if (lengthSquared <= 0f) {
      return from.dst2(point);
    }
    float fraction = MathUtils.clamp(point.cpy().sub(from).dot(movement) / lengthSquared, 0f, 1f);
    return movement.scl(fraction).add(from).dst2(point);
  }

  private static boolean intersectsRectangle(
      Vector2 from, Vector2 to, Vector2 centre, float halfWidth, float halfHeight) {
    Vector2[] vertices = {
      new Vector2(centre.x - halfWidth, centre.y - halfHeight),
      new Vector2(centre.x + halfWidth, centre.y - halfHeight),
      new Vector2(centre.x + halfWidth, centre.y + halfHeight),
      new Vector2(centre.x - halfWidth, centre.y + halfHeight)
    };
    return intersectsConvex(from, to, Vector2.Zero, vertices);
  }
}
