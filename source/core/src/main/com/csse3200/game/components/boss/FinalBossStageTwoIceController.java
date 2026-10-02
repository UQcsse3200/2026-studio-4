package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/** Owns stationary ice cover, its physical lifetime, and the short shatter effect. */
class FinalBossStageTwoIceController {
  private static final int PLACEMENT_ATTEMPTS = 48;
  private static final float HIT_FLASH_DURATION = 0.16f;

  final List<Cover> covers = new ArrayList<>();
  final List<Shatter> shatters = new ArrayList<>();

  private final Entity boss;
  private final Entity player;
  private final FinalBossStageTwoConfig config;
  private final Random random;
  private final Predicate<Rectangle> placementFilter;
  private Rectangle currentArena;
  private float respawnElapsed;
  private boolean initialized;
  private boolean spawnQueued;
  private boolean disposed;
  private long generation;

  FinalBossStageTwoIceController(
      Entity boss, Entity player, FinalBossStageTwoConfig config, Random random) {
    this(boss, player, config, random, area -> true);
  }

  FinalBossStageTwoIceController(
      Entity boss,
      Entity player,
      FinalBossStageTwoConfig config,
      Random random,
      Predicate<Rectangle> placementFilter) {
    this.boss = boss;
    this.player = player;
    this.config = config;
    this.random = random;
    this.placementFilter = placementFilter;
  }

  void update(float delta, Rectangle arenaBounds) {
    if (disposed || !Float.isFinite(delta) || delta < 0f) return;
    for (Cover cover : new ArrayList<>(covers)) {
      cover.lifetimeElapsed += delta;
      if (cover.lifetimeElapsed >= config.iceCoverLifetime) {
        covers.remove(cover);
        disposeCover(cover);
        continue;
      }
      cover.hitFlashRemaining = Math.max(0f, cover.hitFlashRemaining - delta);
      cover.spawnElapsed = Math.min(config.iceSpawnDuration, cover.spawnElapsed + delta);
    }
    for (Shatter shatter : shatters) shatter.elapsed += delta;
    shatters.removeIf(shatter -> shatter.elapsed >= config.iceShatterDuration);
    if (!validBounds(arenaBounds)) return;
    boolean resized = currentArena != null && !currentArena.equals(arenaBounds);
    currentArena = new Rectangle(arenaBounds);

    Rectangle interior = inset(currentArena, config.iceCoverGap);
    for (Cover cover : new ArrayList<>(covers)) {
      boolean actorClampedInside =
          resized && (overlapsActor(cover.bounds, boss) || overlapsActor(cover.bounds, player));
      if ((!contains(interior, cover.bounds) || actorClampedInside) && covers.remove(cover)) {
        disposeCover(cover);
      }
    }

    PhysicsService physicsService = ServiceLocator.getPhysicsService();
    EntityService entities = ServiceLocator.getEntityService();
    if (physicsService == null || entities == null) return;
    PhysicsEngine physics = physicsService.getPhysics();
    if (physics == null || physics.getWorld() == null) return;

    if (!initialized) {
      initialized = true;
      respawnElapsed = 0f;
      requestSpawn(config.iceCoverCount, physics, entities);
      return;
    }
    respawnElapsed += delta;
    if (respawnElapsed >= config.iceCoverRespawnInterval) {
      // A long frame replenishes at most one batch, without accumulating missed spawn attempts.
      respawnElapsed %= config.iceCoverRespawnInterval;
      requestSpawn(config.iceCoverRespawnBatch, physics, entities);
    }
  }

  /**
   * The caller supplies the final wall fixture actually hit, never an intermediate ray candidate.
   */
  boolean hitByFire(Fixture fixture) {
    if (fixture == null || disposed) return false;
    for (Cover cover : new ArrayList<>(covers)) {
      ColliderComponent collider = cover.entity.getComponent(ColliderComponent.class);
      if (collider == null || collider.getFixture() != fixture) continue;
      cover.hitsRemaining--;
      cover.hitFlashRemaining = HIT_FLASH_DURATION;
      if (cover.hitsRemaining <= 0) {
        covers.remove(cover);
        shatters.add(new Shatter(cover.bounds));
        disposeCover(cover);
      }
      return true;
    }
    return false;
  }

  void clear() {
    generation++;
    spawnQueued = false;
    initialized = false;
    respawnElapsed = 0f;
    currentArena = null;
    List<Cover> removed = new ArrayList<>(covers);
    covers.clear();
    shatters.clear();
    for (Cover cover : removed) disposeCover(cover);
  }

  void dispose() {
    if (disposed) return;
    disposed = true;
    clear();
  }

  float getShatterDuration() {
    return config.iceShatterDuration;
  }

  float getSpawnDuration() {
    return config.iceSpawnDuration;
  }

  int getMaxHits() {
    return config.iceCoverHits;
  }

  private void requestSpawn(int count, PhysicsEngine physics, EntityService entities) {
    if (count <= 0 || covers.size() >= config.iceCoverCount || spawnQueued) return;
    World world = physics.getWorld();
    if (!world.isLocked()) {
      spawn(count, physics, entities);
      return;
    }
    spawnQueued = true;
    long requestedGeneration = generation;
    entities.schedule(
        () -> {
          if (requestedGeneration != generation || disposed) return;
          spawnQueued = false;
          PhysicsService currentPhysics = ServiceLocator.getPhysicsService();
          if (currentPhysics == null
              || currentPhysics.getPhysics() != physics
              || ServiceLocator.getEntityService() != entities
              || world.isLocked()) return;
          spawn(count, physics, entities);
        });
  }

  private void spawn(int count, PhysicsEngine physics, EntityService entities) {
    if (disposed || currentArena == null) return;
    Rectangle interior = inset(currentArena, config.iceCoverGap);
    float availableX = interior.width - config.iceCoverWidth;
    float availableY = interior.height - config.iceCoverHeight;
    if (availableX < 0f || availableY < 0f) return;
    for (int index = 0; index < count && covers.size() < config.iceCoverCount; index++) {
      Rectangle placement = null;
      for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
        Rectangle candidate =
            new Rectangle(
                interior.x + random.nextFloat() * availableX,
                interior.y + random.nextFloat() * availableY,
                config.iceCoverWidth,
                config.iceCoverHeight);
        if (isClear(candidate, physics.getWorld())) {
          placement = candidate;
          break;
        }
      }
      if (placement == null) continue;
      covers.add(createCover(placement, physics, entities));
    }
  }

  private boolean isClear(Rectangle candidate, World world) {
    Rectangle clearance = inset(candidate, -config.iceCoverGap);
    if (!placementFilter.test(clearance)) return false;
    if (overlapsActor(clearance, boss) || overlapsActor(clearance, player)) return false;
    for (Cover cover : covers) {
      if (clearance.overlaps(cover.bounds)) return false;
    }
    boolean[] blocked = {false};
    // Broad-phase bounds are deliberately conservative: leave a navigable gap to static walls.
    world.QueryAABB(
        fixture -> {
          if (fixture.getBody().isActive()
              && fixture.getBody().getType() == BodyType.StaticBody
              && !fixture.isSensor()) {
            blocked[0] = true;
            return false;
          }
          return true;
        },
        clearance.x,
        clearance.y,
        clearance.x + clearance.width,
        clearance.y + clearance.height);
    return !blocked[0];
  }

  private Cover createCover(Rectangle bounds, PhysicsEngine physics, EntityService entities) {
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(
        bounds.width / 2f,
        bounds.height / 2f,
        new Vector2(bounds.width / 2f, bounds.height / 2f),
        0f);
    Entity coverEntity =
        new Entity()
            .addComponent(new PhysicsComponent(physics).setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE).setShape(shape));
    coverEntity.setPosition(bounds.x, bounds.y);
    coverEntity.setScale(bounds.width, bounds.height);
    try {
      // These stationary bodies need no entity updates. Their owner creates/disposes them instead
      // of adding temporary cover to the room's entity list during that list's update iteration.
      coverEntity.create();
    } finally {
      shape.dispose(); // Box2D copies the shape into the fixture.
    }
    Cover cover = new Cover(coverEntity, bounds, config.iceCoverHits);
    cover.world = physics.getWorld();
    cover.entities = entities;
    return cover;
  }

  private void disposeCover(Cover cover) {
    if (cover.removalScheduled || cover.world == null) return;
    cover.removalScheduled = true;
    if (cover.world.isLocked()) {
      // Do not disable/deactivate the body during a contact callback. The logical cover is already
      // removed, so further fire hits cannot damage it while safe physical disposal is pending.
      cover.entities.schedule(cover.entity::dispose);
    } else {
      cover.entity.dispose();
    }
  }

  private static boolean overlapsActor(Rectangle rectangle, Entity actor) {
    if (actor == null) return false;
    Vector2 position = actor.getPosition();
    Vector2 scale = actor.getScale();
    return rectangle.overlaps(new Rectangle(position.x, position.y, scale.x, scale.y));
  }

  private static Rectangle inset(Rectangle bounds, float amount) {
    return new Rectangle(
        bounds.x + amount,
        bounds.y + amount,
        bounds.width - 2f * amount,
        bounds.height - 2f * amount);
  }

  private static boolean contains(Rectangle outer, Rectangle inner) {
    return outer.width >= inner.width
        && outer.height >= inner.height
        && inner.x >= outer.x
        && inner.y >= outer.y
        && inner.x + inner.width <= outer.x + outer.width
        && inner.y + inner.height <= outer.y + outer.height;
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

  static class Cover {
    final Entity entity;
    final Rectangle bounds;
    int hitsRemaining;
    float hitFlashRemaining;
    float spawnElapsed;
    private double lifetimeElapsed;
    private World world;
    private EntityService entities;
    private boolean removalScheduled;

    Cover(Entity entity, Rectangle bounds, int hitsRemaining) {
      this.entity = entity;
      this.bounds = new Rectangle(bounds);
      this.hitsRemaining = hitsRemaining;
    }
  }

  static class Shatter {
    final Rectangle bounds;
    float elapsed;

    Shatter(Rectangle bounds) {
      this.bounds = new Rectangle(bounds);
    }
  }
}
