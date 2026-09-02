package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ItemFactory;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Spawns configured enemies and tracks when the room has been cleared. */
public class EnemyManagerComponent extends EntityManagerComponent {
  private final EnemySpawnConfig[] spawnConfigs;
  private final Set<Entity> activeEnemies = new HashSet<>();

  /** Creates an empty manager for tests and rooms with no enemies. */
  public EnemyManagerComponent() {
    this(new EnemySpawnConfig[0]);
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
      case BOMB:
        return NPCFactory.createBombEnemy(target);
      case CHASE:
        return NPCFactory.createChaseEnemy(target, true);
      case FLOATING_DEMON:
        TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
        Vector2 leftPoint = terrain.tileToWorldPosition(spawn.x - 4, spawn.y);
        Vector2 topPoint = terrain.tileToWorldPosition(spawn.x, spawn.y + 3);
        Vector2 rightPoint = terrain.tileToWorldPosition(spawn.x + 4, spawn.y);
        return NPCFactory.createFloatingDemon(
            target, leftPoint, topPoint, rightPoint, this::spawnEntity);
      default:
        throw new IllegalArgumentException("Unsupported enemy type: " + spawn.type);
    }
  }

  /** Tracks an enemy and any children it spawns. Package-private for testing. */
  void track(Entity enemy) {
    activeEnemies.add(enemy);
    enemy
        .getEvents()
        .addListener(
            "entityDied",
            () -> {
              ServiceLocator.getEntityService().schedule(() -> onEnemyDefeated(enemy));
            });
    enemy
        .getEvents()
        .addListener("spawnChildren", (Entity child) -> replaceWithChild(enemy, child));
  }

  private void onEnemyDefeated(Entity enemy) {
    if (activeEnemies.remove(enemy) && activeEnemies.isEmpty()) {
      entity.getEvents().trigger("roomCleared");
    }
    spawnItemDrop(enemy);
  }

  private void replaceWithChild(Entity parent, Entity child) {
    track(child);
    activeEnemies.remove(parent);
    spawnEntity(child);
  }

  private void spawnItemDrop(Entity enemy) {
    Entity item = ItemFactory.createDrop(ItemType.STRENGTH_CHARM, enemy.getPosition());
    spawnEntity(item);
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
}
