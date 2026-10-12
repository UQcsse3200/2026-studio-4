package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/** Finds and reuses clear underground routes around room walls. */
final class SnakeBurrowNavigator {
  private static final float GRID_STEP = 0.35f;
  private static final float RETRY_SECONDS = 0.4f;
  private static final float TARGET_CHANGE = 0.75f;
  private static final float UNBOUNDED_MARGIN = 8f;
  private static final int MAX_GRID_NODES = 16384;
  private static final int MAX_EXPANSIONS = 6000;

  private final SnakeBurrowArena arena;
  private final Rectangle bounds;
  private final List<Vector2> route = new ArrayList<>();
  private final Vector2 plannedTarget = new Vector2();
  private final Vector2 plannedGoal = new Vector2();
  private final Vector2 plannedHalfSize = new Vector2();
  private final Vector2 resolvedRequest = new Vector2();
  private Vector2 resolvedTarget;
  private float retryRemaining;
  private float resolveRemaining;
  private boolean hasPlan;
  private boolean hasResolution;
  private boolean detouring;

  SnakeBurrowNavigator(SnakeBurrowArena arena, Rectangle bounds) {
    this.arena = arena;
    this.bounds = bounds == null ? null : new Rectangle(bounds);
  }

  /**
   * Returns a copied, directly reachable waypoint, or the current position if no route is found.
   * Searches are bounded and cached; failed routes are retried after a short delay.
   */
  Vector2 nextWaypoint(Vector2 from, Vector2 destination, Vector2 halfSize, float delta) {
    float elapsed = Math.max(0f, delta);
    retryRemaining = Math.max(0f, retryRemaining - elapsed);
    resolveRemaining = Math.max(0f, resolveRemaining - elapsed);
    if (!plannedHalfSize.epsilonEquals(halfSize, 0.0001f)) {
      reset();
      plannedHalfSize.set(halfSize);
    }
    Vector2 target = arena.constrain(destination, halfSize);
    if (arena.isPathClear(from, target, halfSize)) {
      return directWaypoint(target);
    }
    Vector2 goal = resolveTarget(target, halfSize);
    if (goal != null && arena.isPathClear(from, goal, halfSize)) {
      return directWaypoint(goal);
    }
    detouring = true;
    if (goal == null) {
      route.clear();
      return from.cpy();
    }
    boolean blocked = !route.isEmpty() && !arena.isPathClear(from, route.get(0), halfSize);
    if (blocked || shouldReplan(target, goal)) {
      plan(from, target, goal, halfSize);
    }
    return selectWaypoint(from, halfSize);
  }

  /** Whether the latest request still requires going around a wall before starting a warning. */
  boolean isDetouring() {
    return detouring;
  }

  private Vector2 directWaypoint(Vector2 target) {
    route.clear();
    hasPlan = false;
    detouring = false;
    return target.cpy();
  }

  private Vector2 resolveTarget(Vector2 target, Vector2 halfSize) {
    float movement = resolvedRequest.dst2(target);
    boolean moved = movement >= 0.01f;
    boolean relocated = movement >= TARGET_CHANGE * TARGET_CHANGE;
    boolean blocked =
        resolvedTarget == null || !arena.isPathClear(resolvedTarget, resolvedTarget, halfSize);
    if (!hasResolution || relocated || (resolveRemaining <= 0f && (moved || blocked))) {
      resolvedTarget = arena.findClearCentre(target, halfSize).orElse(null);
      resolvedRequest.set(target);
      hasResolution = true;
      resolveRemaining = RETRY_SECONDS;
    }
    return resolvedTarget;
  }

  /** Discards a previous encounter's route and retry delay. */
  void reset() {
    route.clear();
    retryRemaining = 0f;
    hasPlan = false;
    hasResolution = false;
    resolvedTarget = null;
    resolveRemaining = 0f;
    detouring = false;
  }

  private boolean shouldReplan(Vector2 target, Vector2 goal) {
    if (!hasPlan) {
      return true;
    }
    return retryRemaining <= 0f
        && (route.isEmpty()
            || plannedTarget.dst2(target) >= TARGET_CHANGE * TARGET_CHANGE
            || plannedGoal.dst2(goal) >= GRID_STEP * GRID_STEP);
  }

  private void plan(Vector2 from, Vector2 target, Vector2 goal, Vector2 halfSize) {
    route.clear();
    hasPlan = true;
    retryRemaining = RETRY_SECONDS;
    plannedTarget.set(target);
    plannedGoal.set(goal);
    plannedHalfSize.set(halfSize);
    if (!arena.isPathClear(from, from, halfSize)) {
      return;
    }
    route.addAll(search(from, goal, halfSize));
  }

  private Vector2 selectWaypoint(Vector2 from, Vector2 halfSize) {
    while (!route.isEmpty() && from.dst2(route.get(0)) < 0.0001f) {
      route.remove(0);
    }
    for (int index = route.size() - 1; index >= 0; index--) {
      Vector2 waypoint = route.get(index);
      if (arena.isPathClear(from, waypoint, halfSize)) {
        route.subList(0, index).clear();
        return waypoint.cpy();
      }
    }
    route.clear();
    return from.cpy();
  }

  private List<Vector2> search(Vector2 from, Vector2 goal, Vector2 halfSize) {
    Grid grid = createGrid(from, goal, halfSize);
    if (grid == null) {
      return List.of();
    }
    PriorityQueue<Visit> open = new PriorityQueue<>();
    seedSearch(grid, from, goal, halfSize, open);
    int expanded = 0;
    while (!open.isEmpty() && expanded < MAX_EXPANSIONS) {
      Visit visit = open.remove();
      if (grid.closed[visit.index()]) {
        continue;
      }
      int index = visit.index();
      grid.closed[index] = true;
      expanded++;
      Vector2 point = grid.point(index);
      if (point.dst2(goal) <= 4f * GRID_STEP * GRID_STEP
          && arena.isPathClear(point, goal, halfSize)) {
        return reconstruct(grid, index, goal);
      }
      addNeighbours(grid, index, point, goal, halfSize, open);
    }
    return List.of();
  }

  private Grid createGrid(Vector2 from, Vector2 goal, Vector2 halfSize) {
    Rectangle area = bounds == null ? fallbackBounds(from, goal) : new Rectangle(bounds);
    area.x += halfSize.x;
    area.y += halfSize.y;
    area.width -= 2f * halfSize.x;
    area.height -= 2f * halfSize.y;
    if (area.width < 0f || area.height < 0f) {
      return null;
    }
    int columns = (int) Math.floor(area.width / GRID_STEP) + 1;
    int rows = (int) Math.floor(area.height / GRID_STEP) + 1;
    if ((long) columns * rows > MAX_GRID_NODES) {
      return null;
    }
    return new Grid(area.x, area.y, columns, rows);
  }

  private Rectangle fallbackBounds(Vector2 from, Vector2 goal) {
    return new Rectangle(
        Math.min(from.x, goal.x) - UNBOUNDED_MARGIN,
        Math.min(from.y, goal.y) - UNBOUNDED_MARGIN,
        Math.abs(from.x - goal.x) + 2f * UNBOUNDED_MARGIN,
        Math.abs(from.y - goal.y) + 2f * UNBOUNDED_MARGIN);
  }

  private void seedSearch(
      Grid grid, Vector2 from, Vector2 goal, Vector2 halfSize, PriorityQueue<Visit> open) {
    int column = Math.round((from.x - grid.x) / GRID_STEP);
    int row = Math.round((from.y - grid.y) / GRID_STEP);
    for (int y = row - 2; y <= row + 2; y++) {
      for (int x = column - 2; x <= column + 2; x++) {
        int index = grid.index(x, y);
        if (index >= 0 && arena.isPathClear(from, grid.point(index), halfSize)) {
          Vector2 point = grid.point(index);
          grid.cost[index] = from.dst(point);
          open.add(new Visit(index, grid.cost[index] + point.dst(goal)));
        }
      }
    }
  }

  private void addNeighbours(
      Grid grid,
      int current,
      Vector2 point,
      Vector2 goal,
      Vector2 halfSize,
      PriorityQueue<Visit> open) {
    int column = current % grid.columns;
    int row = current / grid.columns;
    for (int y = row - 1; y <= row + 1; y++) {
      for (int x = column - 1; x <= column + 1; x++) {
        int next = grid.index(x, y);
        if (next >= 0 && !grid.closed[next]) {
          considerNeighbour(grid, current, next, point, goal, halfSize, open);
        }
      }
    }
  }

  private void considerNeighbour(
      Grid grid,
      int current,
      int next,
      Vector2 point,
      Vector2 goal,
      Vector2 halfSize,
      PriorityQueue<Visit> open) {
    Vector2 candidate = grid.point(next);
    float cost = grid.cost[current] + point.dst(candidate);
    if (cost >= grid.cost[next] || !arena.isPathClear(point, candidate, halfSize)) {
      return;
    }
    grid.cost[next] = cost;
    grid.parent[next] = current;
    open.add(new Visit(next, cost + candidate.dst(goal)));
  }

  private List<Vector2> reconstruct(Grid grid, int end, Vector2 goal) {
    List<Vector2> result = new ArrayList<>();
    result.add(goal.cpy());
    for (int index = end; index >= 0; index = grid.parent[index]) {
      result.add(grid.point(index));
    }
    Collections.reverse(result);
    return result;
  }

  private record Visit(int index, float priority) implements Comparable<Visit> {
    @Override
    public int compareTo(Visit other) {
      return Float.compare(priority, other.priority);
    }
  }

  private static final class Grid {
    private final float x;
    private final float y;
    private final int columns;
    private final int rows;
    private final float[] cost;
    private final int[] parent;
    private final boolean[] closed;

    private Grid(float x, float y, int columns, int rows) {
      this.x = x;
      this.y = y;
      this.columns = columns;
      this.rows = rows;
      cost = new float[columns * rows];
      parent = new int[cost.length];
      closed = new boolean[cost.length];
      Arrays.fill(cost, Float.POSITIVE_INFINITY);
      Arrays.fill(parent, -1);
    }

    private int index(int column, int row) {
      return column < 0 || row < 0 || column >= columns || row >= rows
          ? -1
          : row * columns + column;
    }

    private Vector2 point(int index) {
      return new Vector2(x + (index % columns) * GRID_STEP, y + (index / columns) * GRID_STEP);
    }
  }
}
