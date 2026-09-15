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
import com.csse3200.game.items.WeaponItem;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Spawns configured enemies and tracks when the room has been cleared. */
public class EnemyManagerComponent extends EntityManagerComponent {
  private final EnemySpawnConfig[] spawnConfigs;
  private final Set<Entity> activeEnemies = new HashSet<>();
  private final List<Entity> droppedItems = new ArrayList<>();
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
    this.spawnConfigs = spawnConfigs;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("RoomCreated", this::spawnEnemies);
  }

  /** Spawns each enemy declared by the room. */
  public void spawnEnemies(Entity target) {
    for (EnemySpawnConfig spawn : spawnConfigs) {
      Entity enemy = createEnemy(spawn, target);
      track(enemy);
      spawnEntityAt(enemy, new GridPoint2(spawn.x, spawn.y), true, true);
    }
  }

  private Entity createEnemy(EnemySpawnConfig spawn, Entity target) {
    switch (spawn.type) {
      // Egyptian
      case BEETLE:
        return NPCFactory.createBombEnemy(target, "images/beetle.atlas", 2f);
      case CRAB:
        Entity crab = NPCFactory.createChaseEnemy(target, true, "images/crab.atlas");
        crab.setScale(1.5f, 1.5f);
        crab.getComponent(HitboxComponent.class)
            .setAsBox(
                new Vector2(1f, 0.5f),
                new Vector2(crab.getCenterPosition().x, crab.getCenterPosition().y / 2));
        return crab;
      case MUMMY:
        Entity mummy = NPCFactory.createGiantEnemy(target, "images/mummy.atlas");
        mummy.getComponent(HitboxComponent.class).setAsBox(new Vector2(1f, 1.5f));
        return mummy;
      // Greek
      case GOLEM:
        Entity golem = NPCFactory.createBombEnemy(target, "images/golem.atlas", 2f);
        golem.setScale(1.5F, 1.5F);
        golem
            .getComponent(HitboxComponent.class)
            .setAsBox(
                new Vector2(1, 1),
                new Vector2(golem.getCenterPosition().x, golem.getCenterPosition().y / 2));
        return golem;
      case MEDUSA:
        Entity medusa = NPCFactory.createChaseEnemy(target, true, "images/medusa.atlas");
        medusa.setScale(1.5f, 1.5f);
        medusa.getComponent(HitboxComponent.class).setAsBox(new Vector2(1, 1));
        return medusa;
      case HARPY:
        TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
        Vector2 leftPoint = terrain.tileToWorldPosition(spawn.x - 4, spawn.y);
        Vector2 topPoint = terrain.tileToWorldPosition(spawn.x, spawn.y + 3);
        Vector2 rightPoint = terrain.tileToWorldPosition(spawn.x + 4, spawn.y);
        return NPCFactory.createFloatingDemon(
            target, leftPoint, topPoint, rightPoint, this::spawnEntity, "images/harpy.atlas");
      case CYCLOPS:
        Entity cyclops = NPCFactory.createGiantEnemy(target, "images/cyclops.atlas");
        cyclops
            .getComponent(HitboxComponent.class)
            .setAsBox(
                new Vector2(1f, 1.5f),
                new Vector2(cyclops.getCenterPosition().x, cyclops.getCenterPosition().y / 2));
        return cyclops;
      case CERBERUS:
        TerrainComponent cerberusTerrain = entity.getComponent(TerrainComponent.class);
        Vector2 anchorPoint = cerberusTerrain.tileToWorldPosition(spawn.x, spawn.y);
        return CerberusFactory.createCerberus(
            target, anchorPoint, this::spawnAndTrackCerberusHead, "images/cerberus.atlas");
      case BOW:
        return ItemFactory.createItem(WeaponItem.createWeaponItem(WeaponItem.WeaponType.BOW));
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
  private void spawnAndTrackCerberusHead(Entity head) {
    track(head);
    spawnEntity(head);
  }

  /** Tracks an enemy and any children it spawns. Package-private for testing. */
  void track(Entity enemy) {
    activeEnemies.add(enemy);
    enemy.getEvents().<Entity>addListener("cerberusProjectileSpawned", this::spawnEntity);
    enemy.getEvents().addListener("entityDied", () -> onEnemyDefeated(enemy));
    enemy
        .getEvents()
        .addListener("spawnChildren", (Entity child) -> replaceWithChild(enemy, child));
  }

  private void onEnemyDefeated(Entity enemy) {
    if (activeEnemies.remove(enemy) && activeEnemies.isEmpty()) {
      entity.getEvents().trigger("roomCleared");
    }
    ServiceLocator.getEntityService().schedule(() -> spawnItemDrop(enemy));
  }

  private void replaceWithChild(Entity parent, Entity child) {
    track(child);
    activeEnemies.remove(parent);
    spawnEntity(child);
  }

  private void spawnItemDrop(Entity enemy) {
    Entity item = ItemFactory.createDrop(enemy.getPosition());

    // spawning item should not use the spawnEntity as items are stored in their own list.
    droppedItems.add(item);
    ServiceLocator.getEntityService().register(item);
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
    for (Entity item : droppedItems) {
      item.dispose();
    }
    super.dispose();
  }
}
