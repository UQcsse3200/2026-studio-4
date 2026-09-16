package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Wandering remnants of defeated statues. Owns movement and lifetime, not damage or textures. */
final class FinalBossTornadoController {
  static final int MAX_TORNADOES = 4;
  static final float WIDTH = 2.6f;
  static final float HEIGHT = 1.4625f;
  static final float SPAWN_DURATION = 0.6f;
  static final float DISSOLVE_DURATION = 0.6f;
  static final float WANDER_SPEED = 0.55f;
  static final float CHASE_SPEED = 0.85f;
  static final float CHASE_RADIUS = 2.5f;
  static final float RELEASE_RADIUS = 3.5f;
  static final float STATUE_CLEARANCE = 2.35f;
  static final float PEER_CLEARANCE = 1.6f;
  private static final float EPSILON = 0.00001f;
  private static final float FOOTPRINT = 0.65f;
  private static final float MAX_MOVEMENT_DELTA = 0.25f;
  private static final float SPAWN_RETRY_INTERVAL = 0.25f;
  private static final float[] AVOIDANCE_ANGLES = {0f, 45f, -45f, 90f, -90f, 135f, -135f, 180f};

  final List<Tornado> items = new ArrayList<>();
  private final List<Vector2> pendingSpawns = new ArrayList<>();
  private final Entity target;
  private final Supplier<Rectangle> arena;
  private final BiPredicate<Vector2, Vector2> clearSpace;
  private final Consumer<Vector2> greyBurst;
  private boolean ending;
  private float spawnRetryRemaining;
  private Supplier<List<StatuePath>> statuePaths = List::of;

  FinalBossTornadoController(
      Entity target,
      Supplier<Rectangle> arena,
      BiPredicate<Vector2, Vector2> clearSpace,
      Consumer<Vector2> greyBurst) {
    this.target = target;
    this.arena = arena;
    this.clearSpace = clearSpace;
    this.greyBurst = greyBurst;
  }

  /**
   * Supplies current statue floor points and any floor routes already reserved by their movement.
   */
  void setStatuePaths(Supplier<List<StatuePath>> statuePaths) {
    this.statuePaths = statuePaths;
  }

  /**
   * A statue may move away from an existing intrusion, but must not approach or cross a remnant.
   */
  boolean clearStatueRoute(Vector2 groundFrom, Vector2 groundTo) {
    for (Tornado tornado : items) {
      if (!clearanceAllows(
          groundFrom, groundTo, tornado.position, tornado.position, STATUE_CLEARANCE, true))
        return false;
    }
    return true;
  }

  /** Evade destinations must leave the full visual clearance around every remnant. */
  boolean clearStatuePosition(Vector2 groundPosition) {
    for (Tornado tornado : items) {
      if (groundPosition.dst2(tornado.position) < STATUE_CLEARANCE * STATUE_CLEARANCE) return false;
    }
    return true;
  }

  /** The last statue ends all remnants instead of spawning a fifth one. */
  void statueBroken(Vector2 position, int remainingStatues) {
    if (ending) return;
    if (remainingStatues == 0) {
      ending = true;
      pendingSpawns.clear();
      for (Tornado tornado : items) {
        tornado.dissolving = true;
        tornado.chasing = false;
        tornado.destination = null;
        greyBurst.accept(tornado.position.cpy().add(0f, HEIGHT * 0.5f));
      }
      return;
    }
    if (items.size() + pendingSpawns.size() >= MAX_TORNADOES) return;
    Vector2 spawn = findSpawn(position, null);
    if (spawn != null) {
      items.add(new Tornado(spawn, items.size() * 0.37f));
    } else {
      if (pendingSpawns.isEmpty()) spawnRetryRemaining = SPAWN_RETRY_INTERVAL;
      pendingSpawns.add(position.cpy());
    }
  }

  int activeCount() {
    return (int) items.stream().filter(tornado -> !tornado.dissolving).count();
  }

  void update(float delta, boolean combatActive) {
    if (!Float.isFinite(delta) || delta <= 0f) return;
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    if (stats != null && stats.isDead()) {
      clear();
      return;
    }
    for (Tornado tornado : items) {
      boolean cameraCorrected = recoverIntoCamera(tornado);
      if (tornado.destination != null
          && !clamp(tornado.destination).epsilonEquals(tornado.destination, EPSILON)) {
        resetDestination(tornado);
      }
      tornado.elapsed += delta;
      if (tornado.dissolving) tornado.dissolveElapsed += delta;
      else if (!cameraCorrected && combatActive && tornado.elapsed >= SPAWN_DURATION)
        updateMovement(tornado, delta);
    }
    items.removeIf(tornado -> tornado.dissolving && tornado.dissolveElapsed >= DISSOLVE_DURATION);
    retryPendingSpawns(delta, combatActive);
  }

  void clear() {
    items.clear();
    pendingSpawns.clear();
    spawnRetryRemaining = 0f;
    ending = true;
  }

  /** A temporary reserved route must delay a statue's remnant rather than silently discard it. */
  private void retryPendingSpawns(float delta, boolean combatActive) {
    if (ending || !combatActive || pendingSpawns.isEmpty()) return;
    spawnRetryRemaining = Math.max(0f, spawnRetryRemaining - delta);
    if (spawnRetryRemaining > 0f) return;
    // At most one bounded pass per update, including after a long frame.
    spawnRetryRemaining = SPAWN_RETRY_INTERVAL;
    Iterator<Vector2> pending = pendingSpawns.iterator();
    while (pending.hasNext()) {
      Vector2 spawn = findSpawn(pending.next(), null);
      if (spawn != null) {
        items.add(new Tornado(spawn, items.size() * 0.37f));
        pending.remove();
      }
    }
  }

  /**
   * Camera changes are an explicit containment correction, separate from ordinary slow movement.
   */
  private boolean recoverIntoCamera(Tornado tornado) {
    Vector2 inside = clamp(tornado.position);
    if (inside.epsilonEquals(tornado.position, EPSILON)) return false;
    Vector2 safe = findSpawn(inside, tornado);
    // An entirely obstructed camera cannot satisfy every clearance. Keep the persistent remnant
    // visible inside the view; normal updates will retreat once a safe direction opens.
    tornado.position.set(safe == null ? inside : safe);
    resetDestination(tornado);
    tornado.chasing = false;
    return true;
  }

  private void updateMovement(Tornado tornado, float delta) {
    float movementDelta = Math.min(delta, MAX_MOVEMENT_DELTA);
    if (!positionClear(tornado.position, tornado)) {
      tornado.chasing = false;
      resetDestination(tornado);
      retreat(tornado, WANDER_SPEED * movementDelta);
      return;
    }
    Vector2 player = FinalBossStageThreeComponent.groundPosition(target);
    float radius = tornado.chasing ? RELEASE_RADIUS : CHASE_RADIUS;
    boolean chase =
        !StatusEffectsControllerComponent.isConcealed(target)
            && tornado.position.dst2(player) <= radius * radius
            && positionClear(clamp(player), tornado);
    if (chase != tornado.chasing) {
      resetDestination(tornado);
    }
    tornado.chasing = chase;
    tornado.retargetRemaining = Math.max(0f, tornado.retargetRemaining - delta);
    if (chase) tornado.destination = clamp(player);
    else if (tornado.destination == null
        || tornado.retargetRemaining == 0f
        || tornado.position.dst2(tornado.destination) < 0.04f) chooseWanderDestination(tornado);
    if (tornado.destination == null) return;
    float speed = chase ? CHASE_SPEED : WANDER_SPEED;
    if (!advance(tornado, speed * movementDelta)) resetDestination(tornado);
  }

  private void chooseWanderDestination(Tornado tornado) {
    tornado.destination = null;
    tornado.retargetRemaining = MathUtils.random(1.2f, 2.5f);
    float startAngle = MathUtils.random(360f);
    for (int attempt = 0; attempt < 12; attempt++) {
      Vector2 offset = new Vector2(MathUtils.random(1.5f, 3f), 0f);
      Vector2 candidate =
          clamp(tornado.position.cpy().add(offset.rotateDeg(startAngle + attempt * 30f)));
      if (candidate.dst2(tornado.position) > 0.04f
          && routeClear(tornado, tornado.position, candidate, false)) {
        tornado.destination = candidate;
        return;
      }
    }
  }

  private boolean advance(Tornado tornado, float distance) {
    Vector2 direction = tornado.destination.cpy().sub(tornado.position);
    if (direction.isZero(0.001f)) return true;
    direction.setLength(Math.min(distance, direction.len()));
    for (float angle : AVOIDANCE_ANGLES) {
      Vector2 candidate = clamp(tornado.position.cpy().add(direction.cpy().rotateDeg(angle)));
      if (candidate.dst2(tornado.position) > 0.000001f
          && routeClear(tornado, tornado.position, candidate, false)) {
        tornado.position.set(candidate);
        return true;
      }
    }
    return false;
  }

  /** When another actor intrudes, prefer a slow step out instead of remaining permanently stuck. */
  private void retreat(Tornado tornado, float distance) {
    Vector2 best = null;
    float bestClearance = -1f;
    for (int direction = 0; direction < 24; direction++) {
      Vector2 candidate =
          clamp(tornado.position.cpy().add(new Vector2(distance, 0f).rotateDeg(direction * 15f)));
      if (!routeClear(tornado, tornado.position, candidate, true)) continue;
      float clearance = separationScore(candidate, tornado);
      if (clearance > bestClearance) {
        best = candidate;
        bestClearance = clearance;
      }
    }
    if (best != null) tornado.position.set(best);
  }

  private static void resetDestination(Tornado tornado) {
    tornado.destination = null;
    tornado.retargetRemaining = 0f;
  }

  private boolean positionClear(Vector2 position, Tornado owner) {
    return routeClear(owner, position, position, false);
  }

  private boolean routeClear(Tornado owner, Vector2 from, Vector2 to, boolean allowEscape) {
    Vector2 swept =
        new Vector2(FOOTPRINT + Math.abs(to.x - from.x), FOOTPRINT + Math.abs(to.y - from.y));
    if (!clearSpace.test(from.cpy().add(to).scl(0.5f), swept)) return false;
    for (StatuePath path : statuePaths.get()) {
      if (!clearanceAllows(from, to, path.from, path.to, STATUE_CLEARANCE, allowEscape))
        return false;
    }
    for (Tornado peer : items) {
      if (peer != owner
          && !clearanceAllows(from, to, peer.position, peer.position, PEER_CLEARANCE, allowEscape))
        return false;
    }
    return true;
  }

  private float separationScore(Vector2 position, Tornado owner) {
    float nearest = Float.POSITIVE_INFINITY;
    for (StatuePath path : statuePaths.get()) {
      nearest =
          Math.min(
              nearest,
              pointSegmentDistance2(position, path.from, path.to)
                  / (STATUE_CLEARANCE * STATUE_CLEARANCE));
    }
    for (Tornado peer : items) {
      if (peer != owner)
        nearest =
            Math.min(nearest, position.dst2(peer.position) / (PEER_CLEARANCE * PEER_CLEARANCE));
    }
    return nearest;
  }

  private static boolean clearanceAllows(
      Vector2 from,
      Vector2 to,
      Vector2 obstacleFrom,
      Vector2 obstacleTo,
      float clearance,
      boolean allowEscape) {
    float sweptDistance = segmentDistance2(from, to, obstacleFrom, obstacleTo);
    float required = clearance * clearance;
    if (sweptDistance + EPSILON >= required) return true;
    if (!allowEscape) return false;
    float initial = pointSegmentDistance2(from, obstacleFrom, obstacleTo);
    return initial < required
        && sweptDistance + EPSILON >= initial
        && pointSegmentDistance2(to, obstacleFrom, obstacleTo) > initial + EPSILON;
  }

  private static float segmentDistance2(
      Vector2 from, Vector2 to, Vector2 otherFrom, Vector2 otherTo) {
    if (Intersector.intersectSegments(from, to, otherFrom, otherTo, null)) return 0f;
    return Math.min(
        Math.min(
            pointSegmentDistance2(from, otherFrom, otherTo),
            pointSegmentDistance2(to, otherFrom, otherTo)),
        Math.min(
            pointSegmentDistance2(otherFrom, from, to), pointSegmentDistance2(otherTo, from, to)));
  }

  private static float pointSegmentDistance2(Vector2 point, Vector2 from, Vector2 to) {
    float length2 = from.dst2(to);
    if (length2 == 0f) return point.dst2(from);
    float projection =
        MathUtils.clamp(
            ((point.x - from.x) * (to.x - from.x) + (point.y - from.y) * (to.y - from.y)) / length2,
            0f,
            1f);
    return Vector2.dst2(
        point.x,
        point.y,
        from.x + projection * (to.x - from.x),
        from.y + projection * (to.y - from.y));
  }

  private Vector2 findSpawn(Vector2 requested, Tornado owner) {
    Vector2 origin = clamp(requested);
    if (positionClear(origin, owner)) return origin;
    for (int ring = 1; ring <= 8; ring++) {
      for (int direction = 0; direction < 16; direction++) {
        Vector2 candidate =
            clamp(origin.cpy().add(new Vector2(ring * 0.35f, 0f).rotateDeg(direction * 22.5f)));
        if (positionClear(candidate, owner)) return candidate;
      }
    }
    return findCameraSpace(origin, owner);
  }

  /** Search the whole view when a nearby statue or reserved route blocks the original spawn. */
  private Vector2 findCameraSpace(Vector2 origin, Tornado owner) {
    Rectangle bounds = arena.get();
    Vector2 best = null;
    float bestDistance = Float.POSITIVE_INFINITY;
    int columns = Math.max(1, (int) Math.ceil(bounds.width / 0.5f));
    int rows = Math.max(1, (int) Math.ceil(bounds.height / 0.5f));
    for (int column = 0; column <= columns; column++) {
      for (int row = 0; row <= rows; row++) {
        Vector2 candidate =
            clamp(
                new Vector2(
                    bounds.x + bounds.width * column / columns,
                    bounds.y + bounds.height * row / rows));
        float distance = origin.dst2(candidate);
        if (distance < bestDistance && positionClear(candidate, owner)) {
          bestDistance = distance;
          best = candidate;
        }
      }
    }
    return best;
  }

  private Vector2 clamp(Vector2 position) {
    Rectangle bounds = arena.get();
    float insetX = Math.min(bounds.width * 0.5f, WIDTH * 0.5f + 0.1f);
    float minY = bounds.y + Math.min(bounds.height * 0.5f, FOOTPRINT * 0.5f + 0.1f);
    float maxY = Math.max(minY, bounds.y + bounds.height - HEIGHT - 0.1f);
    return new Vector2(
        MathUtils.clamp(position.x, bounds.x + insetX, bounds.x + bounds.width - insetX),
        MathUtils.clamp(position.y, minY, maxY));
  }

  /** Defensive snapshot of a live statue's ground location and any reserved movement segment. */
  record StatuePath(Vector2 from, Vector2 to) {
    StatuePath {
      from = from.cpy();
      to = to.cpy();
    }

    @Override
    public Vector2 from() {
      return from.cpy();
    }

    @Override
    public Vector2 to() {
      return to.cpy();
    }
  }

  static final class Tornado {
    final Vector2 position;
    final float animationOffset;
    Vector2 destination;
    float elapsed;
    float retargetRemaining;
    float dissolveElapsed;
    boolean dissolving;
    boolean chasing;

    Tornado(Vector2 position, float animationOffset) {
      this.position = position.cpy();
      this.animationOffset = animationOffset;
    }
  }
}
