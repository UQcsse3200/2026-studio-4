package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Owns Stage 2 fireball flight and firing cadence without creating physics or render entities. */
final class FinalBossStageTwoFireController {
  private static final float CAST_DURATION = 0.3f;
  private static final float NO_HIT = Float.POSITIVE_INFINITY;

  /** The first solid wall hit, including a start inside a wall; infinity means no hit. */
  @FunctionalInterface
  interface WallQuery {
    float firstHitFraction(Vector2 from, Vector2 to);
  }

  final List<Fireball> fireballs = new ArrayList<>();
  final List<Impact> impacts = new ArrayList<>();
  private final FinalBossStageTwoConfig config;
  private final Random random;
  private float shotRemaining;
  private float castRemaining;
  private long nextId = 1L;
  private long clearVersion;

  FinalBossStageTwoFireController(FinalBossStageTwoConfig config, Random random) {
    this.config = Objects.requireNonNull(config, "Fireball config must not be null");
    this.random = Objects.requireNonNull(random, "Fireball random source must not be null");
    config.validate();
    shotRemaining = config.fireballInitialDelay;
  }

  /**
   * Moves existing fireballs even while firing is paused. The caller provides the player's two
   * positions from the same interval, so a fast player cannot cross a stationary fireball unseen.
   * New volleys are limited to one per update rather than accumulating a long-frame backlog.
   */
  void update(
      float delta,
      boolean canFire,
      Vector2 origin,
      Vector2 playerBefore,
      Vector2 playerNow,
      float playerRadius,
      Rectangle bounds,
      WallQuery walls,
      Runnable hitPlayer) {
    if (!Float.isFinite(delta) || delta <= 0f) return;
    if (!validBounds(bounds)) {
      clear();
      return;
    }
    castRemaining = Math.max(0f, castRemaining - delta);
    for (Impact impact : impacts) impact.elapsed += delta;
    impacts.removeIf(impact -> impact.elapsed >= config.fireImpactDuration);

    long version = clearVersion;
    // The damage callback can end the encounter or consume another projectile. A snapshot avoids
    // invalidating an iterator; membership checks prevent later hits from already-consumed balls.
    for (Fireball fireball : new ArrayList<>(fireballs)) {
      if (!fireballs.contains(fireball)) continue;
      advanceFireball(
          fireball, delta, playerBefore, playerNow, playerRadius, bounds, walls, hitPlayer);
      if (clearVersion != version) return;
    }

    if (!canFire) {
      // The next firing window opens immediately, without carrying missed volleys through a pause.
      shotRemaining = 0f;
      return;
    }
    shotRemaining -= delta;
    if (shotRemaining > 0f) return;
    shotRemaining = config.fireVolleyInterval;
    if (validPoint(origin) && bounds.contains(origin) && wallFraction(walls, origin, origin) > 1f) {
      fireVolley(origin);
    }
  }

  private void advanceFireball(
      Fireball fireball,
      float delta,
      Vector2 playerBefore,
      Vector2 playerNow,
      float playerRadius,
      Rectangle bounds,
      WallQuery walls,
      Runnable hitPlayer) {
    float remaining = config.fireballLifetime - fireball.elapsed;
    if (remaining <= 0f) {
      fireballs.remove(fireball);
      return;
    }
    // Sweeping only the remaining lifetime also keeps a very long frame from overflowing flight
    // coordinates or hitting a target reached after the projectile should have expired.
    float flightTime = Math.min(delta, remaining);
    Vector2 from = fireball.position.cpy();
    Vector2 to = from.cpy().mulAdd(fireball.velocity, flightTime);
    float wallHit = wallFraction(walls, from, to);
    float boundaryHit = boundaryFraction(from, to, bounds);
    float expiryHit = delta >= remaining ? 1f : NO_HIT;
    float blockedAt = Math.min(wallHit, Math.min(boundaryHit, expiryHit));
    float playerHit = NO_HIT;
    if (hitPlayer != null
        && validPoint(playerBefore)
        && validPoint(playerNow)
        && Float.isFinite(playerRadius)
        && playerRadius >= 0f) {
      Vector2 playerEnd = playerBefore.cpy().lerp(playerNow, flightTime / delta);
      playerHit =
          circleHitFraction(
              from.cpy().sub(playerBefore),
              to.cpy().sub(playerEnd),
              config.fireballRadius + playerRadius);
    }

    if (playerHit < blockedAt && playerHit <= 1f) {
      fireball.position.set(from.lerp(to, playerHit));
      fireballs.remove(fireball);
      impacts.add(new Impact(fireball.position));
      // Remove before the callback, including when damage synchronously changes the phase.
      hitPlayer.run();
    } else if (blockedAt <= 1f) {
      fireball.position.set(from.lerp(to, blockedAt));
      fireballs.remove(fireball);
      if (wallHit <= boundaryHit && wallHit <= expiryHit)
        impacts.add(new Impact(fireball.position));
    } else {
      fireball.position.set(to);
      fireball.elapsed += flightTime;
    }
  }

  private void fireVolley(Vector2 origin) {
    int available = config.maxFireballs - fireballs.size();
    if (available <= 0) return;
    int pattern = random.nextInt(3);
    int count = pattern == 0 ? 6 : pattern == 1 ? 4 : 5;
    float baseAngle = random.nextFloat() * 360f;
    for (int i = 0; i < count && i < available; i++) {
      float angle =
          switch (pattern) {
            case 0 -> baseAngle + i * 60f;
            case 1 -> baseAngle + (i - 1.5f) * 24f;
            default -> baseAngle + i * 72f + (random.nextFloat() - 0.5f) * 24f;
          };
      Vector2 velocity = new Vector2(config.fireballSpeed, 0f).rotateDeg(angle);
      fireballs.add(new Fireball(nextId++, origin, velocity));
    }
    castRemaining = CAST_DURATION;
  }

  /** Consumes one projectile by stable ID; callers can add the appropriate cancellation effect. */
  boolean consume(long id) {
    return fireballs.removeIf(fireball -> fireball.id == id);
  }

  float getCastRemaining() {
    return castRemaining;
  }

  float getImpactDuration() {
    return config.fireImpactDuration;
  }

  /** Clears an encounter, preserving unique IDs so an old ID cannot consume a later projectile. */
  void clear() {
    clearVersion++;
    fireballs.clear();
    impacts.clear();
    castRemaining = 0f;
    shotRemaining = config.fireballInitialDelay;
  }

  private static float wallFraction(WallQuery walls, Vector2 from, Vector2 to) {
    if (walls == null) return NO_HIT;
    float fraction = walls.firstHitFraction(from, to);
    return Float.isFinite(fraction) && fraction >= 0f && fraction <= 1f ? fraction : NO_HIT;
  }

  /** First intersection with a circle centred on zero, in relative bullet/player coordinates. */
  private static float circleHitFraction(Vector2 from, Vector2 to, float radius) {
    double c = (double) from.x * from.x + (double) from.y * from.y - (double) radius * radius;
    if (c <= 0d) return 0f;
    double dx = (double) to.x - from.x;
    double dy = (double) to.y - from.y;
    double a = dx * dx + dy * dy;
    if (a == 0d) return NO_HIT;
    double b = 2d * (from.x * dx + from.y * dy);
    double discriminant = b * b - 4d * a * c;
    if (discriminant < 0d) return NO_HIT;
    double fraction = (-b - Math.sqrt(discriminant)) / (2d * a);
    return fraction >= 0d && fraction <= 1d ? (float) fraction : NO_HIT;
  }

  /** Boundary collision takes priority over targets outside the camera's battle rectangle. */
  private static float boundaryFraction(Vector2 from, Vector2 to, Rectangle bounds) {
    if (!bounds.contains(from)) return 0f;
    float fraction = NO_HIT;
    float dx = to.x - from.x;
    float dy = to.y - from.y;
    if (dx > 0f) fraction = Math.min(fraction, (bounds.x + bounds.width - from.x) / dx);
    else if (dx < 0f) fraction = Math.min(fraction, (bounds.x - from.x) / dx);
    if (dy > 0f) fraction = Math.min(fraction, (bounds.y + bounds.height - from.y) / dy);
    else if (dy < 0f) fraction = Math.min(fraction, (bounds.y - from.y) / dy);
    return fraction >= 0f && fraction <= 1f ? MathUtils.clamp(fraction, 0f, 1f) : NO_HIT;
  }

  private static boolean validBounds(Rectangle bounds) {
    return bounds != null
        && Float.isFinite(bounds.x)
        && Float.isFinite(bounds.y)
        && Float.isFinite(bounds.width)
        && Float.isFinite(bounds.height)
        && bounds.width > 0f
        && bounds.height > 0f;
  }

  private static boolean validPoint(Vector2 point) {
    return point != null && Float.isFinite(point.x) && Float.isFinite(point.y);
  }

  static final class Fireball {
    final long id;
    final Vector2 position;
    final Vector2 velocity;
    float elapsed;

    Fireball(long id, Vector2 position, Vector2 velocity) {
      this.id = id;
      this.position = position.cpy();
      this.velocity = velocity.cpy();
    }
  }

  static final class Impact {
    final Vector2 position;
    float elapsed;

    Impact(Vector2 position) {
      this.position = position.cpy();
    }
  }
}
