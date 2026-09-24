package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.boss.FinalBossMovementComponent;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.CerberusFactory;
import com.csse3200.game.entities.factories.FinalBossFactory;
import com.csse3200.game.entities.factories.ItemFactory;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Spawns configured enemies and tracks when the room has been cleared. */
public class EnemyManagerComponent extends EntityManagerComponent {
  private final EnemySpawnConfig[] spawnConfigs;
  private final Set<Entity> activeEnemies = new HashSet<>();
  private final ItemFactory itemFactory;
  private boolean disposed;
  private CameraComponent camera;

  /** Creates an empty manager for tests and rooms with no enemies. */
  public EnemyManagerComponent() {
    this(new EnemySpawnConfig[0]);
  }

  public EnemyManagerComponent(EnemySpawnConfig[] spawnConfigs, CameraComponent camera) {
    this(spawnConfigs);
    this.camera = camera;
  }

  public EnemyManagerComponent(EnemySpawnConfig[] spawnConfigs) {
    this(spawnConfigs, new ItemFactory());
  }

  /** Uses an injectable item factory for enemy drops. */
  EnemyManagerComponent(EnemySpawnConfig[] spawnConfigs, ItemFactory itemFactory) {
    this.spawnConfigs = spawnConfigs;
    this.itemFactory = Objects.requireNonNull(itemFactory);
  }

  @Override
  public void create() {
    entity.getEvents().addListener("RoomCreated", this::spawnEnemies);
  }

  /** Spawns each enemy declared by the room. */
  public void spawnEnemies(Entity target) {
    for (EnemySpawnConfig spawn : spawnConfigs) {
      Entity enemy = createEnemy(spawn, target);
      track(enemy, spawn.type.name());
      spawnEntityAt(enemy, new GridPoint2(spawn.x, spawn.y), true, true);
    }
  }

  private Entity createEnemy(EnemySpawnConfig spawn, Entity target) {

    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    Vector2 leftPoint = terrain.tileToWorldPosition(spawn.x - 4, spawn.y);
    Vector2 topPoint = terrain.tileToWorldPosition(spawn.x, spawn.y + 3);
    Vector2 rightPoint = terrain.tileToWorldPosition(spawn.x + 4, spawn.y);

    switch (spawn.type) {
      // Egyptian
      case BEETLE:
        Entity beetle = NPCFactory.createBombEnemy(target, "images/beetle.atlas", 2f);
        beetle.setScale(0.75f, 0.75f);
        return beetle;
      case CRAB:
        Entity crab = NPCFactory.createChaseEnemy(target, true, "images/crab.atlas");
        crab.setScale(1.5f, 1f);
        crab.getComponent(HitboxComponent.class)
            .setAsBox(
                new Vector2(1f, 0.5f),
                new Vector2(crab.getCenterPosition().x, crab.getCenterPosition().y / 2));
        return crab;
      case WASP:
        return NPCFactory.createFloatingDemon(
            target, leftPoint, topPoint, rightPoint, this::spawnEntity, "images/wasp.atlas");
      case MUMMY:
        Entity mummy = NPCFactory.createGiantEnemy(target, "images/mummy.atlas");
        mummy
            .getComponent(HitboxComponent.class)
            .setAsBox(new Vector2(1f, 1.5f), mummy.getCenterPosition());
        PhysicsUtils.setScaledCollider(mummy, 0.3f, 0.3f);
        return mummy;
      case SNAKE_MINI_BOSS:
        return NPCFactory.createSnakeMiniBoss(target);
      // Greek
      case GOLEM:
        Entity golem = NPCFactory.createBombEnemy(target, "images/golem.atlas", 2f);
        golem.setScale(0.9F, 0.7F);
        golem
            .getComponent(HitboxComponent.class)
            .setAsBox(
                new Vector2(1, 1),
                new Vector2(golem.getCenterPosition().x, golem.getCenterPosition().y / 2));
        PhysicsUtils.setScaledCollider(golem, 0.3f, 0.3f);
        return golem;
      case MEDUSA:
        Entity medusa = NPCFactory.createChaseEnemy(target, true, "images/medusa.atlas");
        medusa.setScale(1f, 1f);
        medusa.getComponent(HitboxComponent.class).setAsBox(new Vector2(1, 1));
        return medusa;
      case HARPY:
        return NPCFactory.createFloatingDemon(
            target, leftPoint, topPoint, rightPoint, this::spawnEntity, "images/harpy.atlas");
      case CYCLOPS:
        Entity cyclops = NPCFactory.createGiantEnemy(target, "images/cyclops.atlas");
        cyclops.setScale(1.5f, 1.5f);
        cyclops
            .getComponent(HitboxComponent.class)
            .setAsBox(
                new Vector2(1f, 1f),
                new Vector2(cyclops.getCenterPosition().x, cyclops.getCenterPosition().y / 2));
        PhysicsUtils.setScaledCollider(cyclops, 0.3f, 0.3f);
        return cyclops;
      case CERBERUS:
        TerrainComponent cerberusTerrain = entity.getComponent(TerrainComponent.class);
        Vector2 anchorPoint = cerberusTerrain.tileToWorldPosition(spawn.x, spawn.y);
        return CerberusFactory.createCerberus(
            target,
            anchorPoint,
            head -> spawnAndTrackCerberusHead(head, spawn.type.name()),
            "images/cerberus.atlas");
      case FINAL_BOSS:
        Entity boss = FinalBossFactory.createFinalBoss(target, this::spawnEntity);
        if (camera != null) {
          boss.getComponent(FinalBossMovementComponent.class).setCamera(camera.getCamera());
        }
        return boss;
      default:
        throw new IllegalArgumentException("Unsupported enemy type: " + spawn.type);
    }
  }

  /** Tracks an enemy and any children it spawns. Package-private for testing. */
  private void spawnAndTrackCerberusHead(Entity head, String enemyType) {
    track(head, enemyType);
    spawnEntity(head);
  }

  /** Tracks an enemy and any children it spawns. Package-private for testing. */
  void track(Entity enemy) {
    track(enemy, null);
  }

  void track(Entity enemy, String enemyType) {
    if (disposed || !activeEnemies.add(enemy)) {
      return;
    }
    enemy.getEvents().<Entity>addListener("cerberusProjectileSpawned", this::spawnEntity);
    enemy.getEvents().addListener("entityDied", () -> onEnemyDefeated(enemy, enemyType));
    enemy
        .getEvents()
        .addListener("finalBossEncounterCompleted", () -> onEnemyDefeated(enemy, enemyType));
    enemy
        .getEvents()
        .addListener("spawnChildren", (Entity child) -> replaceWithChild(enemy, child, enemyType));
  }

  private void onEnemyDefeated(Entity enemy, String enemyType) {
    if (disposed || !activeEnemies.remove(enemy)) {
      return;
    }
    // Capture before deferred disposal or room changes can move/remove the enemy.
    Vector2 position = enemy.getPosition().cpy();
    spawnEnemyDrops(enemyType, position);
    if (activeEnemies.isEmpty()) {
      entity.getEvents().trigger("roomCleared");
    }
  }

  private void replaceWithChild(Entity parent, Entity child, String enemyType) {
    track(child, enemyType);
    activeEnemies.remove(parent);
    spawnEntity(child);
  }

  /** Creates and registers room-owned items after the current update is safe for spawning. */
  public void spawnItem(ItemType itemType, int quantity, Vector2 position) {
    if (disposed) {
      return;
    }
    Objects.requireNonNull(itemType, "itemType cannot be null");
    Vector2 spawnPosition = Objects.requireNonNull(position, "position cannot be null").cpy();
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    ServiceLocator.getEntityService()
        .schedule(
            () -> {
              if (disposed) {
                return;
              }
              for (Entity item :
                  ItemFactory.createDrops(itemType.createItem(1), quantity, spawnPosition)) {
                spawnEntity(item);
              }
            });
  }

  /** Requests enemy-specific items from the factory and registers them with this room. */
  private void spawnEnemyDrops(String enemyType, Vector2 position) {
    ServiceLocator.getEntityService()
        .schedule(
            () -> {
              if (disposed) {
                return;
              }
              for (Entity item : itemFactory.createEnemyDrops(enemyType, position)) {
                spawnEntity(item);
              }
            });
  }

  /** Returns whether the room has any living enemies. */
  public boolean isCleared() {
    return activeEnemies.isEmpty();
  }

  /** Defeats all living enemies for temporary transition testing. */
  public void clear() {
    for (Entity enemy : new ArrayList<>(activeEnemies)) {
      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      if (stats != null && !stats.isDead()) {
        stats.setHealth(0);
      }
    }
  }

  /**
   * Iterates through the active enemies and calls scale on their {@link CombatStatsComponent}
   *
   * <p>This method should be called during the room creatation.
   */
  public void scale(int mult) {
    for (Entity enemy : new ArrayList<>(activeEnemies)) {
      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      if (stats != null) {
        stats.scale(mult);
      }
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    activeEnemies.clear();
    super.dispose();
  }
}
