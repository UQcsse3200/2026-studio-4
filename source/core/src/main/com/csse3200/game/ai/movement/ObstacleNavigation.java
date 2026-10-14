package com.csse3200.game.ai.movement;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/** Local ground navigation using solid obstacle fixtures and the mover's actual footprint. */
public final class ObstacleNavigation {
  private static final float SKIN = 0.005f;
  private static final float CORNER_CLEARANCE = 0.04f;
  private static final float DETOUR_MARGIN = 3f;
  private static final int MAX_SEARCH_NODES = 512;

  private ObstacleNavigation() {}

  /** Checks the entire collision box along a straight movement, ignoring walkable sensors. */
  public static boolean isClear(Entity mover, Vector2 destination) {
    Vector2 start = mover.getPosition();
    return isClear(start, destination, obstacles(mover, start, destination, 0f));
  }

  /**
   * Finds a route around nearby obstacles. Waypoints are entity origins, as required by the physics
   * movement controller. An unreachable destination returns an empty route.
   */
  public static List<Vector2> findPath(Entity mover, Vector2 destination) {
    Vector2 start = mover.getPosition();
    List<Rectangle> obstacles = obstacles(mover, start, destination, DETOUR_MARGIN);
    if (isClear(start, destination, obstacles)) {
      return List.of(destination.cpy());
    }
    Rectangle searchArea = area(start, destination, DETOUR_MARGIN);
    List<Vector2> nodes = new ArrayList<>();
    nodes.add(start);
    nodes.add(destination.cpy());
    for (Rectangle obstacle : obstacles) {
      addCorners(nodes, obstacle, obstacles, searchArea);
      if (nodes.size() > MAX_SEARCH_NODES) {
        return List.of();
      }
    }
    return search(nodes, obstacles);
  }

  private static void addCorners(
      List<Vector2> nodes, Rectangle box, List<Rectangle> obstacles, Rectangle searchArea) {
    float left = box.x - CORNER_CLEARANCE;
    float right = box.x + box.width + CORNER_CLEARANCE;
    float bottom = box.y - CORNER_CLEARANCE;
    float top = box.y + box.height + CORNER_CLEARANCE;
    for (Vector2 corner :
        List.of(
            new Vector2(left, bottom),
            new Vector2(left, top),
            new Vector2(right, bottom),
            new Vector2(right, top))) {
      if (searchArea.contains(corner) && isClear(corner, corner, obstacles)) {
        nodes.add(corner);
      }
    }
  }

  private record Candidate(int index, float estimate) {}

  private static List<Vector2> search(List<Vector2> nodes, List<Rectangle> obstacles) {
    float[] costs = new float[nodes.size()];
    Arrays.fill(costs, Float.POSITIVE_INFINITY);
    int[] previous = new int[nodes.size()];
    Arrays.fill(previous, -1);
    boolean[] visited = new boolean[nodes.size()];
    PriorityQueue<Candidate> open =
        new PriorityQueue<>(Comparator.comparingDouble(Candidate::estimate));
    costs[0] = 0f;
    open.add(new Candidate(0, nodes.getFirst().dst(nodes.get(1))));
    while (!open.isEmpty()) {
      Candidate current = open.remove();
      int index = current.index();
      if (visited[index]) {
        continue;
      }
      if (index == 1) {
        return reconstruct(nodes, previous);
      }
      visited[index] = true;
      expand(index, nodes, obstacles, costs, previous, visited, open);
    }
    return List.of();
  }

  private static void expand(
      int index,
      List<Vector2> nodes,
      List<Rectangle> obstacles,
      float[] costs,
      int[] previous,
      boolean[] visited,
      PriorityQueue<Candidate> open) {
    Vector2 from = nodes.get(index);
    for (int next = 1; next < nodes.size(); next++) {
      float cost = costs[index] + from.dst(nodes.get(next));
      if (visited[next] || cost >= costs[next] || !isClear(from, nodes.get(next), obstacles)) {
        continue;
      }
      costs[next] = cost;
      previous[next] = index;
      open.add(new Candidate(next, cost + nodes.get(next).dst(nodes.get(1))));
    }
  }

  private static List<Vector2> reconstruct(List<Vector2> nodes, int[] previous) {
    List<Vector2> route = new ArrayList<>();
    for (int index = 1; index != 0; index = previous[index]) {
      route.add(nodes.get(index).cpy());
    }
    Collections.reverse(route);
    return route;
  }

  private static boolean isClear(Vector2 from, Vector2 to, List<Rectangle> obstacles) {
    for (Rectangle obstacle : obstacles) {
      if (obstacle.contains(from)
          || obstacle.contains(to)
          || Intersector.intersectSegmentRectangle(from, to, obstacle)) {
        return false;
      }
    }
    return true;
  }

  private static Rectangle area(Vector2 from, Vector2 to, float margin) {
    return new Rectangle(
        Math.min(from.x, to.x) - margin,
        Math.min(from.y, to.y) - margin,
        Math.abs(to.x - from.x) + 2f * margin,
        Math.abs(to.y - from.y) + 2f * margin);
  }

  private static List<Rectangle> obstacles(Entity mover, Vector2 from, Vector2 to, float margin) {
    List<Rectangle> result = new ArrayList<>();
    if (ServiceLocator.getPhysicsService() == null) {
      return result;
    }
    ColliderComponent collider = mover.getComponent(ColliderComponent.class);
    Fixture movingFixture = collider == null ? null : collider.getFixture();
    Rectangle footprint = movingFixture == null ? new Rectangle() : bounds(movingFixture);
    if (movingFixture != null) {
      footprint.x -= from.x;
      footprint.y -= from.y;
    }
    Rectangle query = area(from, to, margin + SKIN);
    ServiceLocator.getPhysicsService()
        .getPhysics()
        .getWorld()
        .QueryAABB(
            fixture -> {
              if (isObstacle(fixture, movingFixture)) {
                Rectangle box = bounds(fixture);
                // Minkowski expansion: a point route now accounts for the whole mover footprint.
                box.x -= footprint.x + footprint.width + SKIN;
                box.y -= footprint.y + footprint.height + SKIN;
                box.width += footprint.width + 2f * SKIN;
                box.height += footprint.height + 2f * SKIN;
                result.add(box);
              }
              return true;
            },
            query.x + footprint.x,
            query.y + footprint.y,
            query.x + query.width + footprint.x + footprint.width,
            query.y + query.height + footprint.y + footprint.height);
    return result;
  }

  private static boolean isObstacle(Fixture fixture, Fixture mover) {
    if (fixture.isSensor()
        || !PhysicsLayer.contains(fixture.getFilterData().categoryBits, PhysicsLayer.OBSTACLE)) {
      return false;
    }
    return mover == null
        || (fixture.getBody() != mover.getBody()
            && (fixture.getFilterData().maskBits & mover.getFilterData().categoryBits) != 0
            && (mover.getFilterData().maskBits & fixture.getFilterData().categoryBits) != 0);
  }

  private static Rectangle bounds(Fixture fixture) {
    Shape shape = fixture.getShape();
    if (shape instanceof CircleShape circle) {
      Vector2 center = fixture.getBody().getWorldPoint(circle.getPosition());
      float radius = circle.getRadius();
      return new Rectangle(center.x - radius, center.y - radius, 2f * radius, 2f * radius);
    }
    int count = vertexCount(shape);
    Vector2 vertex = new Vector2();
    float minX = Float.POSITIVE_INFINITY;
    float minY = Float.POSITIVE_INFINITY;
    float maxX = Float.NEGATIVE_INFINITY;
    float maxY = Float.NEGATIVE_INFINITY;
    for (int i = 0; i < count; i++) {
      vertex(shape, i, vertex);
      Vector2 point = fixture.getBody().getWorldPoint(vertex);
      minX = Math.min(minX, point.x);
      minY = Math.min(minY, point.y);
      maxX = Math.max(maxX, point.x);
      maxY = Math.max(maxY, point.y);
    }
    return new Rectangle(minX, minY, maxX - minX, maxY - minY);
  }

  private static int vertexCount(Shape shape) {
    if (shape instanceof PolygonShape polygon) {
      return polygon.getVertexCount();
    }
    if (shape instanceof ChainShape chain) {
      return chain.getVertexCount();
    }
    return 2;
  }

  private static void vertex(Shape shape, int index, Vector2 vertex) {
    if (shape instanceof PolygonShape polygon) {
      polygon.getVertex(index, vertex);
    } else if (shape instanceof ChainShape chain) {
      chain.getVertex(index, vertex);
    } else if (shape instanceof EdgeShape edge) {
      if (index == 0) {
        edge.getVertex1(vertex);
      } else {
        edge.getVertex2(vertex);
      }
    }
  }
}
