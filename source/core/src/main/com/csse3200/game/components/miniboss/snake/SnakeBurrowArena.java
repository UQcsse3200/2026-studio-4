package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Optional;

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
   * Finds clear space at the constrained requested position or within 2.8 world units of it. Checks
   * the entire footprint against solid static fixtures, ignoring players and sensors. Call during a
   * normal update, outside a physics callback. If physics is unavailable, only the room bounds
   * apply. Returns empty rather than placing the Snake in a blocked location.
   */
  public Optional<Vector2> findClearCentre(Vector2 desired, Vector2 halfSize) {
    if (!fitsRoom(halfSize)) {
      return Optional.empty();
    }
    Vector2 origin = constrain(desired, halfSize);
    if (isClear(origin, halfSize)) {
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
        if (isClear(candidate, halfSize)) {
          return Optional.of(candidate);
        }
      }
    }
    return Optional.empty();
  }

  private boolean fitsRoom(Vector2 halfSize) {
    return bounds == null || (halfSize.x * 2f <= bounds.width && halfSize.y * 2f <= bounds.height);
  }

  private boolean isClear(Vector2 centre, Vector2 halfSize) {
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
              if (!fixture.isSensor() && fixture.getBody().getType() == BodyType.StaticBody) {
                clear[0] = false;
                return false;
              }
              return true;
            },
            centre.x - halfSize.x,
            centre.y - halfSize.y,
            centre.x + halfSize.x,
            centre.y + halfSize.y);
    return clear[0];
  }
}
