package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Optional;
import java.util.function.Predicate;

/** Keeps the Snake's burrow position and emergence footprint inside clear room space. */
public final class SnakeBurrowArena {
  private static final int SEARCH_RINGS = 8;
  private static final int SEARCH_DIRECTIONS = 16;
  private static final float SEARCH_STEP = 0.35f;

  private final Rectangle bounds;

  /** Creates an arena, or leaves room bounds unrestricted when {@code bounds} is null. */
  public SnakeBurrowArena(Rectangle bounds) {
    this.bounds = bounds == null ? null : new Rectangle(bounds);
  }

  /**
   * Returns a copied centre constrained by the supplied half-size. An oversized footprint is
   * centred along the undersized room axis; {@link #findClearCentre} will reject that footprint.
   */
  public Vector2 constrain(Vector2 centre, Vector2 halfSize) {
    if (bounds == null) {
      return centre.cpy();
    }
    float insetX = Math.min(halfSize.x, bounds.width / 2f);
    float insetY = Math.min(halfSize.y, bounds.height / 2f);
    return new Vector2(
        MathUtils.clamp(centre.x, bounds.x + insetX, bounds.x + bounds.width - insetX),
        MathUtils.clamp(centre.y, bounds.y + insetY, bounds.y + bounds.height - insetY));
  }

  /**
   * Checks the footprint along the complete straight path between two centres. Both endpoints must
   * fit inside the room, and the swept footprint must avoid solid static fixture geometry.
   */
  public boolean isPathClear(Vector2 from, Vector2 to, Vector2 halfSize) {
    if (!containsFootprint(from, halfSize) || !containsFootprint(to, halfSize)) {
      return false;
    }
    return isSweepClear(from, to, halfSize);
  }

  /**
   * Moves towards a constrained destination without crossing walls. A blocked diagonal tries
   * horizontal movement, then vertical movement; a fully blocked move keeps the original centre.
   * The returned vector is independent of both inputs.
   */
  public Vector2 constrainMovement(Vector2 from, Vector2 desired, Vector2 halfSize) {
    Vector2 destination = constrain(desired, halfSize);
    if (isPathClear(from, destination, halfSize)) {
      return destination;
    }
    Vector2 horizontal = new Vector2(destination.x, from.y);
    if (isPathClear(from, horizontal, halfSize)) {
      return horizontal;
    }
    Vector2 vertical = new Vector2(from.x, destination.y);
    if (isPathClear(from, vertical, halfSize)) {
      return vertical;
    }
    return from.cpy();
  }

  /**
   * Finds clear space at the constrained requested position or within 2.8 world units of it. Checks
   * the entire footprint against solid static fixtures, ignoring players and sensors. Call during a
   * normal update, outside a physics callback. If physics is unavailable, only the room bounds
   * apply. Returns empty rather than placing the Snake in a blocked location.
   */
  public Optional<Vector2> findClearCentre(Vector2 desired, Vector2 halfSize) {
    return findCentre(desired, halfSize, centre -> isClear(centre, halfSize));
  }

  /**
   * Repairs an overlapping footprint at the start of a burrow. The final footprint must be clear,
   * and its centre must remain on the same side of every wall. This recovery allows the initial
   * footprint to overlap a wall but never moves a centre through one.
   */
  public Optional<Vector2> findClearStartCentre(Vector2 current, Vector2 halfSize) {
    return findCentre(
        current,
        halfSize,
        candidate ->
            isClear(candidate, halfSize)
                && SnakePoisonCollision.wallFraction(current, candidate, 0f, null) > 1f);
  }

  /**
   * Finds a nearby emergence centre whose entire approach from {@code from} clears solid static
   * fixtures. Uses the same search area as {@link #findClearCentre} and rejects destinations across
   * a wall even when their final footprint is clear.
   */
  public Optional<Vector2> findReachableCentre(Vector2 from, Vector2 desired, Vector2 halfSize) {
    return findCentre(desired, halfSize, centre -> isPathClear(from, centre, halfSize));
  }

  private Optional<Vector2> findCentre(
      Vector2 desired, Vector2 halfSize, Predicate<Vector2> acceptable) {
    if (!fitsRoom(halfSize)) {
      return Optional.empty();
    }
    Vector2 origin = constrain(desired, halfSize);
    if (acceptable.test(origin)) {
      return Optional.of(origin);
    }
    for (int ring = 1; ring <= SEARCH_RINGS; ring++) {
      for (int direction = 0; direction < SEARCH_DIRECTIONS; direction++) {
        float angle = direction * MathUtils.PI2 / SEARCH_DIRECTIONS;
        Vector2 candidate =
            new Vector2(MathUtils.cos(angle), MathUtils.sin(angle))
                .scl(ring * SEARCH_STEP)
                .add(origin);
        candidate = constrain(candidate, halfSize);
        if (acceptable.test(candidate)) {
          return Optional.of(candidate);
        }
      }
    }
    return Optional.empty();
  }

  private boolean fitsRoom(Vector2 halfSize) {
    return bounds == null || (halfSize.x * 2f <= bounds.width && halfSize.y * 2f <= bounds.height);
  }

  private boolean containsFootprint(Vector2 centre, Vector2 halfSize) {
    return bounds == null
        || (centre.x - halfSize.x >= bounds.x
            && centre.y - halfSize.y >= bounds.y
            && centre.x + halfSize.x <= bounds.x + bounds.width
            && centre.y + halfSize.y <= bounds.y + bounds.height);
  }

  private boolean isClear(Vector2 centre, Vector2 halfSize) {
    return isSweepClear(centre, centre, halfSize);
  }

  private boolean isSweepClear(Vector2 from, Vector2 to, Vector2 halfSize) {
    PhysicsService service = ServiceLocator.getPhysicsService();
    if (service == null) {
      return true;
    }
    boolean[] clear = {true};
    service
        .getPhysics()
        .getWorld()
        .QueryAABB(
            fixture -> {
              if (!fixture.isSensor()
                  && fixture.getBody().getType() == BodyType.StaticBody
                  && SnakeBurrowCollision.intersects(fixture, from, to, halfSize)) {
                clear[0] = false;
                return false;
              }
              return true;
            },
            Math.min(from.x, to.x) - halfSize.x,
            Math.min(from.y, to.y) - halfSize.y,
            Math.max(from.x, to.x) + halfSize.x,
            Math.max(from.y, to.y) + halfSize.y);
    return clear[0];
  }
}
