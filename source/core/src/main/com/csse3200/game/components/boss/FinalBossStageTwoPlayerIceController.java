package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Encounter-owned homing ice shots, paid for from the player's persistent ice-energy slots. */
final class FinalBossStageTwoPlayerIceController {
  private static final float NO_HIT = Float.POSITIVE_INFINITY;
  private static final float ENERGY_EPSILON = 0.000001f;

  final List<Shot> shots = new ArrayList<>();
  final List<Impact> impacts = new ArrayList<>();

  private final FinalBossStageTwoConfig config;
  private float cooldown;
  private long nextId = 1L;
  private long clearVersion;
  private boolean disposed;

  FinalBossStageTwoPlayerIceController(FinalBossStageTwoConfig config) {
    this.config = Objects.requireNonNull(config);
    config.validate();
  }

  void update(
      float delta,
      boolean firing,
      Vector2 origin,
      FinalBossStageTwoProjectileTarget target,
      Rectangle bounds,
      FinalBossStageTwoFireController.WallQuery walls,
      FinalBossStageTwoPickupController energy) {
    if (disposed || !Float.isFinite(delta) || delta <= 0f) return;
    if (!validBounds(bounds)) {
      clear();
      return;
    }
    // Releasing the key never resets the cooldown, so rapid tapping cannot bypass the cadence.
    cooldown = Math.max(0f, cooldown - delta);
    for (Impact impact : impacts) impact.elapsed += delta;
    impacts.removeIf(impact -> impact.elapsed >= config.iceImpactDuration);
    long version = clearVersion;
    for (Shot shot : new ArrayList<>(shots)) {
      if (!shots.contains(shot)) continue;
      advance(shot, delta, target, bounds, walls);
      if (version != clearVersion) return;
    }

    Vector2 bossNow = target == null ? null : target.currentPosition();
    float cost = 1f / config.iceShotsPerCharge;
    if (!firing
        || cooldown > 0f
        || shots.size() >= config.maxIceProjectiles
        || energy == null
        || energy.getChargeFraction(0) + energy.getChargeFraction(1) < cost - ENERGY_EPSILON
        || !validPoint(origin)
        || !validPoint(bossNow)
        || !bounds.contains(origin)
        || wallFraction(walls, origin, origin) <= 1f) return;

    Vector2 velocity = bossNow.cpy().sub(origin);
    if (velocity.isZero()) velocity.set(1f, 0f);
    velocity.setLength(config.iceProjectileSpeed);
    Shot shot = new Shot(nextId++, origin, velocity);
    if (energy.consumeEnergy(cost) < cost - ENERGY_EPSILON) return;
    shots.add(shot);
    cooldown = config.iceFireInterval;
    // At most one new shot, born at the end of the update, even after a long stall.
  }

  private void advance(
      Shot shot,
      float delta,
      FinalBossStageTwoProjectileTarget target,
      Rectangle bounds,
      FinalBossStageTwoFireController.WallQuery walls) {
    float remaining = config.iceProjectileLifetime - shot.elapsed;
    if (remaining <= 0f) {
      shots.remove(shot);
      return;
    }
    float flightTime = Math.min(delta, remaining);
    steer(shot, target == null ? null : target.currentPosition(), flightTime);
    Vector2 from = shot.position.cpy();
    Vector2 to = from.cpy().mulAdd(shot.velocity, flightTime);
    float wallHit = wallFraction(walls, from, to);
    float boundaryHit = boundaryFraction(from, to, bounds);
    float expiryHit = delta >= remaining ? 1f : NO_HIT;
    float blockedAt = Math.min(wallHit, Math.min(boundaryHit, expiryHit));
    float bossHit = NO_HIT;
    if (target != null && target.canBeHit()) {
      Vector2 bossEnd =
          target.previousPosition().cpy().lerp(target.currentPosition(), flightTime / delta);
      bossHit =
          circleHitFraction(
              from.cpy().sub(target.previousPosition()),
              to.cpy().sub(bossEnd),
              config.iceProjectileRadius + target.radius());
    }
    if (bossHit < blockedAt && bossHit <= 1f) {
      shot.position.set(from.lerp(to, bossHit));
      shots.remove(shot);
      impacts.add(new Impact(shot.position));
      // Health events may synchronously end the encounter. Remove before notifying the owner.
      target.onHit().run();
    } else if (blockedAt <= 1f) {
      shot.position.set(from.lerp(to, blockedAt));
      shots.remove(shot);
      if (wallHit <= boundaryHit && wallHit <= expiryHit) {
        impacts.add(new Impact(shot.position));
      }
      // Ice stops at cover without ever invoking the fire-only wall damage callback.
    } else {
      shot.position.set(to);
      shot.elapsed += flightTime;
    }
  }

  private void steer(Shot shot, Vector2 target, float delta) {
    if (!validPoint(target)) return;
    Vector2 desired = target.cpy().sub(shot.position);
    if (desired.isZero()) return;
    if (shot.velocity.isZero()) {
      shot.velocity.set(desired).setLength(config.iceProjectileSpeed);
      return;
    }
    float difference = (desired.angleDeg() - shot.velocity.angleDeg() + 540f) % 360f - 180f;
    float maximumTurn = config.iceHomingTurnRate * delta;
    shot.velocity
        .rotateDeg(MathUtils.clamp(difference, -maximumTurn, maximumTurn))
        .setLength(config.iceProjectileSpeed);
  }

  boolean consume(long id) {
    return shots.removeIf(shot -> shot.id == id);
  }

  float getImpactDuration() {
    return config.iceImpactDuration;
  }

  /** IDs remain unique across encounters so stale cancellation IDs cannot consume new shots. */
  void clear() {
    clearVersion++;
    shots.clear();
    impacts.clear();
    cooldown = 0f;
  }

  void dispose() {
    if (disposed) return;
    disposed = true;
    clear();
  }

  private static float wallFraction(
      FinalBossStageTwoFireController.WallQuery walls, Vector2 from, Vector2 to) {
    if (walls == null) return NO_HIT;
    float fraction = walls.firstHitFraction(from, to);
    return Float.isFinite(fraction) && fraction >= 0f && fraction <= 1f ? fraction : NO_HIT;
  }

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

  private static float boundaryFraction(Vector2 from, Vector2 to, Rectangle bounds) {
    if (!bounds.contains(from)) return 0f;
    float fraction = NO_HIT;
    float dx = to.x - from.x;
    float dy = to.y - from.y;
    if (dx > 0f) fraction = Math.min(fraction, (bounds.x + bounds.width - from.x) / dx);
    else if (dx < 0f) fraction = Math.min(fraction, (bounds.x - from.x) / dx);
    if (dy > 0f) fraction = Math.min(fraction, (bounds.y + bounds.height - from.y) / dy);
    else if (dy < 0f) fraction = Math.min(fraction, (bounds.y - from.y) / dy);
    return fraction >= 0f && fraction <= 1f ? fraction : NO_HIT;
  }

  private static boolean validPoint(Vector2 point) {
    return point != null && Float.isFinite(point.x) && Float.isFinite(point.y);
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

  static final class Shot {
    final long id;
    final Vector2 position;
    final Vector2 velocity;
    float elapsed;

    Shot(long id, Vector2 position, Vector2 velocity) {
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
