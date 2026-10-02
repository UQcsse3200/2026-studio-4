package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Owns ground ice gems and two persistent, sequentially consumed energy slots. */
final class FinalBossStageTwoPickupController {
  private static final int MAX_CHARGES = 2;
  private static final int PLACEMENT_ATTEMPTS = 48;
  private static final float ENERGY_EPSILON = 0.000001f;

  @FunctionalInterface
  interface PlacementQuery {
    /** Whether the entire requested clearance is free of solid obstacles. */
    boolean isClear(Rectangle clearance);
  }

  final List<Pickup> pickups = new ArrayList<>();
  final List<Burst> bursts = new ArrayList<>();

  private final FinalBossStageTwoConfig config;
  private final Random random;
  private final float[] charges = new float[MAX_CHARGES];
  private final int[] chargeOrder = new int[MAX_CHARGES];
  private int chargeCount;
  private double spawnElapsed;
  private Rectangle arenaBounds;
  private boolean initialized;
  private boolean disposed;

  FinalBossStageTwoPickupController(FinalBossStageTwoConfig config, Random random) {
    this.config = Objects.requireNonNull(config);
    this.random = Objects.requireNonNull(random);
    config.validate();
  }

  void update(
      float delta,
      Rectangle arena,
      Rectangle bossBounds,
      Rectangle playerBounds,
      Vector2 previousPlayerCentre,
      PlacementQuery placement) {
    if (disposed || !Float.isFinite(delta) || delta < 0f) return;
    if (!validBounds(arena)) {
      clear();
      return;
    }
    arenaBounds = new Rectangle(arena);
    for (Burst burst : bursts) burst.elapsed += delta;
    bursts.removeIf(burst -> burst.elapsed >= config.icePickupEffectDuration);
    pickups.removeIf(pickup -> !insideArena(pickup.position));

    Vector2 current = validBounds(playerBounds) ? playerBounds.getCenter(new Vector2()) : null;
    Vector2 previous = validPoint(previousPlayerCentre) ? previousPlayerCentre : current;
    double radius =
        current == null
            ? 0d
            : config.icePickupRadius + Math.min(playerBounds.width, playerBounds.height) * 0.3f;
    List<Pickup> existing = new ArrayList<>(pickups);
    // If a dash crosses several gems with one free slot, the first gem reached wins.
    existing.sort(
        Comparator.comparingDouble(pickup -> entryFraction(pickup, previous, current, radius)));
    for (Pickup pickup : existing) {
      double remaining = config.icePickupLifetime - pickup.elapsed;
      double entry = entryFraction(pickup, previous, current, radius);
      if (chargeCount < MAX_CHARGES && entry <= 1d && entry * delta < remaining) {
        pickups.remove(pickup);
        int slot = charges[0] <= 0f ? 0 : 1;
        charges[slot] = 1f;
        chargeOrder[chargeCount++] = slot;
        bursts.add(new Burst(pickup.position));
      } else {
        pickup.elapsed += delta;
        if (pickup.elapsed >= config.icePickupLifetime) pickups.remove(pickup);
      }
    }

    // New gems exist only at the end of this update, so an earlier dash cannot collect them.
    if (!initialized) {
      initialized = true;
      spawnElapsed = 0d;
      spawn(bossBounds, playerBounds, placement);
      return;
    }
    spawnElapsed += delta;
    if (spawnElapsed >= config.icePickupSpawnInterval) {
      spawnElapsed %= config.icePickupSpawnInterval;
      spawn(bossBounds, playerBounds, placement);
    }
  }

  /** Returns the actual energy spent, draining the oldest slot before the next slot. */
  float consumeEnergy(float amount) {
    if (disposed || !Float.isFinite(amount) || amount <= 0f) return 0f;
    float consumed = 0f;
    while (amount > 0f && chargeCount > 0) {
      int slot = chargeOrder[0];
      float spent = Math.min(amount, charges[slot]);
      charges[slot] -= spent;
      amount -= spent;
      consumed += spent;
      // Fractions such as one third must not leave an occupied slot with rounding-only energy.
      if (charges[slot] <= ENERGY_EPSILON) {
        charges[slot] = 0f;
        chargeOrder[0] = chargeOrder[1];
        chargeOrder[1] = 0;
        chargeCount--;
      }
    }
    return consumed;
  }

  float getChargeFraction(int slot) {
    return slot >= 0 && slot < MAX_CHARGES ? charges[slot] : 0f;
  }

  int getChargeCount() {
    return chargeCount;
  }

  float getPickupLifetime() {
    return config.icePickupLifetime;
  }

  float getEffectDuration() {
    return config.icePickupEffectDuration;
  }

  Rectangle getArenaBounds() {
    return arenaBounds == null ? null : new Rectangle(arenaBounds);
  }

  /** Used by later ice-cover spawns; their caller already includes the required gap. */
  boolean isClearOfPickups(Rectangle area) {
    for (Pickup pickup : pickups) {
      if (area.overlaps(pickupBounds(pickup.position))) return false;
    }
    return true;
  }

  void clear() {
    pickups.clear();
    bursts.clear();
    charges[0] = 0f;
    charges[1] = 0f;
    chargeOrder[0] = 0;
    chargeOrder[1] = 0;
    chargeCount = 0;
    spawnElapsed = 0d;
    arenaBounds = null;
    initialized = false;
  }

  void dispose() {
    if (disposed) return;
    disposed = true;
    clear();
  }

  private void spawn(Rectangle bossBounds, Rectangle playerBounds, PlacementQuery placement) {
    if (chargeCount >= MAX_CHARGES || pickups.size() >= config.icePickupCount) return;
    float margin = config.icePickupRadius + config.icePickupGap;
    float width = arenaBounds.width - 2f * margin;
    float height = arenaBounds.height - 2f * margin;
    if (width < 0f || height < 0f) return;
    for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
      Vector2 position =
          new Vector2(
              arenaBounds.x + margin + random.nextFloat() * width,
              arenaBounds.y + margin + random.nextFloat() * height);
      Rectangle clearance =
          new Rectangle(position.x - margin, position.y - margin, margin * 2f, margin * 2f);
      if ((validBounds(bossBounds) && clearance.overlaps(bossBounds))
          || (validBounds(playerBounds) && clearance.overlaps(playerBounds))
          || !isClearOfPickups(clearance)
          || (placement != null && !placement.isClear(clearance))) continue;
      pickups.add(new Pickup(position));
      return;
    }
  }

  private Rectangle pickupBounds(Vector2 position) {
    float radius = config.icePickupRadius;
    return new Rectangle(position.x - radius, position.y - radius, radius * 2f, radius * 2f);
  }

  private boolean insideArena(Vector2 position) {
    float radius = config.icePickupRadius;
    return position.x - radius >= arenaBounds.x
        && position.y - radius >= arenaBounds.y
        && position.x + radius <= arenaBounds.x + arenaBounds.width
        && position.y + radius <= arenaBounds.y + arenaBounds.height;
  }

  private static double entryFraction(Pickup pickup, Vector2 from, Vector2 to, double radius) {
    if (!validPoint(from) || !validPoint(to)) return Double.POSITIVE_INFINITY;
    double x = (double) from.x - pickup.position.x;
    double y = (double) from.y - pickup.position.y;
    double c = x * x + y * y - radius * radius;
    if (c <= 0d) return 0d;
    double dx = (double) to.x - from.x;
    double dy = (double) to.y - from.y;
    double a = dx * dx + dy * dy;
    if (a == 0d) return Double.POSITIVE_INFINITY;
    double b = 2d * (x * dx + y * dy);
    double discriminant = b * b - 4d * a * c;
    if (discriminant < 0d) return Double.POSITIVE_INFINITY;
    double entry = (-b - Math.sqrt(discriminant)) / (2d * a);
    return entry >= 0d && entry <= 1d ? entry : Double.POSITIVE_INFINITY;
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

  static class Pickup {
    final Vector2 position;
    double elapsed;

    Pickup(Vector2 position) {
      this.position = position.cpy();
    }
  }

  static class Burst {
    final Vector2 position;
    float elapsed;

    Burst(Vector2 position) {
      this.position = position.cpy();
    }
  }
}
