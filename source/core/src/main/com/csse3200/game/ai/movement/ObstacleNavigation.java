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
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
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
    return isClear(start, destination, obstacles(mover, start, destination, 0f, null, false));
  }

  /** Includes solid bodies in pursuit checks, while allowing contact with the intended target. */
  public static boolean isClear(Entity mover, Vector2 destination, Entity target) {
    Vector2 start = mover.getPosition();
    return isClear(start, destination, obstacles(mover, start, destination, 0f, target, true));
  }

  /** Pursues a reachable approach position when the target stands too close to a wall or crowd. */
  public static List<Vector2> findPath(Entity mover, Entity target) {
    Vector2 start = mover.getPosition();
    Vector2 destination = target.getPosition();
    List<Rectangle> obstacles = obstacles(mover, start, destination, DETOUR_MARGIN, target, true);
    return findPath(start, destination, obstacles, approachPositions(destination, obstacles));
  }

  /** Finds a short, collision-free retreat when a lower-id neighbour has right of way. */
  public static Vector2 yieldPosition(Entity mover, Entity target) {
    if (ServiceLocator.getPhysicsService() == null) {
      return null;
    }
    Entity neighbour = yieldNeighbour(mover, target);
    if (neighbour == null) {
      return null;
    }
    Vector2 position = mover.getPosition();
    Vector2 away = position.cpy().sub(neighbour.getPosition());
    if (away.isZero()) {
      away.set(1f, 0f);
    }
    away.nor().scl(0.65f);
    for (float angle : new float[] {0f, 45f, -45f, 90f, -90f}) {
      Vector2 candidate = away.cpy().rotateDeg(angle).add(position);
      if (isClear(mover, candidate, target)) {
        return candidate;
      }
    }
    return null;
  }

  private static Entity yieldNeighbour(Entity mover, Entity target) {
    Entity[] neighbour = {null};
    Vector2 position = mover.getPosition();
    ServiceLocator.getPhysicsService()
        .getPhysics()
        .getWorld()
        .QueryAABB(
            fixture -> {
              Object data = fixture.getBody().getUserData();
              if (!fixture.isSensor() && data instanceof BodyUserData owner) {
                Entity other = owner.entity;
                if (other != null
                    && other != target
                    && other.getId() < mover.getId()
                    && other.getComponent(PhysicsMovementComponent.class) != null
                    && other.getPosition().dst2(position) < 2.25f
                    && (neighbour[0] == null || other.getId() < neighbour[0].getId())) {
                  neighbour[0] = other;
                }
              }
              return true;
            },
            position.x - 1.5f,
            position.y - 1.5f,
            position.x + 1.5f,
            position.y + 1.5f);
    return neighbour[0];
  }

  /**
   * Finds a route around nearby obstacles. Waypoints are entity origins, as required by the physics
   * movement controller. An unreachable destination returns an empty route.
   */
  public static List<Vector2> findPath(Entity mover, Vector2 destination) {
    Vector2 start = mover.getPosition();
    List<Rectangle> obstacles = obstacles(mover, start, destination, DETOUR_MARGIN, null, false);
    return findPath(start, destination, obstacles, List.of(destination.cpy()));
  }

  private static List<Vector2> findPath(
      Vector2 start, Vector2 destination, List<Rectangle> obstacles, List<Vector2> goals) {
    if (goals.isEmpty()) {
      return List.of();
    }
    if (isClear(start, destination, obstacles)) {
      return List.of(destination.cpy());
    }
    Rectangle searchArea = area(start, destination, DETOUR_MARGIN);
    List<Vector2> nodes = new ArrayList<>();
    nodes.add(start);
    nodes.addAll(goals);
    for (Rectangle obstacle : obstacles) {
      addCorners(nodes, obstacle, obstacles, searchArea);
      if (nodes.size() > MAX_SEARCH_NODES) {
        return List.of();
      }
    }
    return search(nodes, obstacles, goals.size());
  }

  private static List<Vector2> approachPositions(Vector2 destination, List<Rectangle> obstacles) {
    if (isClear(destination, destination, obstacles)) {
      return List.of(destination.cpy());
    }
    List<Vector2> candidates = new ArrayList<>();
    for (Rectangle box : obstacles) {
      addApproach(
          candidates, new Vector2(box.x - CORNER_CLEARANCE, destination.y), destination, obstacles);
      addApproach(
          candidates,
          new Vector2(box.x + box.width + CORNER_CLEARANCE, destination.y),
          destination,
          obstacles);
      addApproach(
          candidates, new Vector2(destination.x, box.y - CORNER_CLEARANCE), destination, obstacles);
      addApproach(
          candidates,
          new Vector2(destination.x, box.y + box.height + CORNER_CLEARANCE),
          destination,
          obstacles);
    }
    // Corners formed by multiple walls may require an offset on both axes.
    for (float radius = 0.2f; radius <= 1.01f; radius += 0.2f) {
      for (int direction = 0; direction < 8; direction++) {
        Vector2 candidate = new Vector2(radius, 0f).rotateDeg(direction * 45f).add(destination);
        addApproach(candidates, candidate, destination, obstacles);
      }
    }
    candidates.sort(Comparator.comparingDouble(destination::dst2));
    if (candidates.isEmpty()) {
      return List.of();
    }
    float nearest = candidates.getFirst().dst(destination) + CORNER_CLEARANCE;
    return candidates.stream().filter(point -> point.dst(destination) <= nearest).limit(8).toList();
  }

  private static void addApproach(
      List<Vector2> candidates, Vector2 point, Vector2 destination, List<Rectangle> obstacles) {
    if (point.dst2(destination) <= 1f && isClear(point, point, obstacles)) {
      candidates.add(point);
    }
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

  private record SearchState(
      float[] costs, int[] previous, boolean[] visited, PriorityQueue<Candidate> open) {}

  private static List<Vector2> search(
      List<Vector2> nodes, List<Rectangle> obstacles, int goalCount) {
    float[] costs = new float[nodes.size()];
    Arrays.fill(costs, Float.POSITIVE_INFINITY);
    int[] previous = new int[nodes.size()];
    Arrays.fill(previous, -1);
    boolean[] visited = new boolean[nodes.size()];
    PriorityQueue<Candidate> open =
        new PriorityQueue<>(Comparator.comparingDouble(Candidate::estimate));
    SearchState state = new SearchState(costs, previous, visited, open);
    costs[0] = 0f;
    open.add(new Candidate(0, remaining(nodes.getFirst(), nodes, goalCount)));
    while (!open.isEmpty()) {
      Candidate current = open.remove();
      int index = current.index();
      if (visited[index]) {
        continue;
      }
      if (index > 0 && index <= goalCount) {
        return reconstruct(nodes, previous, index);
      }
      visited[index] = true;
      expand(index, nodes, obstacles, state, goalCount);
    }
    return List.of();
  }

  private static void expand(
      int index, List<Vector2> nodes, List<Rectangle> obstacles, SearchState state, int goalCount) {
    Vector2 from = nodes.get(index);
    for (int next = 1; next < nodes.size(); next++) {
      float cost = state.costs()[index] + from.dst(nodes.get(next));
      if (state.visited()[next]
          || cost >= state.costs()[next]
          || !isClear(from, nodes.get(next), obstacles)) {
        continue;
      }
      state.costs()[next] = cost;
      state.previous()[next] = index;
      state.open().add(new Candidate(next, cost + remaining(nodes.get(next), nodes, goalCount)));
    }
  }

  private static float remaining(Vector2 from, List<Vector2> nodes, int goalCount) {
    float distance = Float.POSITIVE_INFINITY;
    for (int i = 1; i <= goalCount; i++) {
      distance = Math.min(distance, from.dst(nodes.get(i)));
    }
    return distance;
  }

  private static List<Vector2> reconstruct(List<Vector2> nodes, int[] previous, int goal) {
    List<Vector2> route = new ArrayList<>();
    for (int index = goal; index != 0; index = previous[index]) {
      route.add(nodes.get(index).cpy());
    }
    Collections.reverse(route);
    return route;
  }

  private static boolean isClear(Vector2 from, Vector2 to, List<Rectangle> obstacles) {
    for (Rectangle obstacle : obstacles) {
      if (obstacle.contains(from)) {
        if (!exitsNearestSide(from, to, obstacle)) {
          return false;
        }
      } else if (obstacle.contains(to)
          || Intersector.intersectSegmentRectangle(from, to, obstacle)) {
        return false;
      }
    }
    return true;
  }

  private static boolean exitsNearestSide(Vector2 from, Vector2 to, Rectangle box) {
    float left = from.x - box.x;
    float right = box.x + box.width - from.x;
    float bottom = from.y - box.y;
    float top = box.y + box.height - from.y;
    float nearest = Math.min(Math.min(left, right), Math.min(bottom, top)) + SKIN;
    // Overlapping spawn positions or a small contact correction must permit retreat, never a
    // shortcut through the far side of the obstacle. Box2D still resolves the actual movement.
    return (left <= nearest && to.x < box.x)
        || (right <= nearest && to.x > box.x + box.width)
        || (bottom <= nearest && to.y < box.y)
        || (top <= nearest && to.y > box.y + box.height);
  }

  private static Rectangle area(Vector2 from, Vector2 to, float margin) {
    return new Rectangle(
        Math.min(from.x, to.x) - margin,
        Math.min(from.y, to.y) - margin,
        Math.abs(to.x - from.x) + 2f * margin,
        Math.abs(to.y - from.y) + 2f * margin);
  }

  private static List<Rectangle> obstacles(
      Entity mover, Vector2 from, Vector2 to, float margin, Entity target, boolean includeBodies) {
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
              if (isObstacle(fixture, movingFixture, target, includeBodies)) {
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

  private static boolean isObstacle(
      Fixture fixture, Fixture mover, Entity target, boolean includeBodies) {
    Object data = fixture.getBody().getUserData();
    if (fixture.isSensor()
        || (data instanceof BodyUserData owner && target != null && owner.entity == target)) {
      return false;
    }
    if (!includeBodies
        && !PhysicsLayer.contains(fixture.getFilterData().categoryBits, PhysicsLayer.OBSTACLE)) {
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
