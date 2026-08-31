package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.SplitComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FloatingDemonConfig;
import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.utils.math.RandomUtils;
import java.util.Random;

/** Spawns a random group of ghosts when its room is started. */
public class EnemyManagerComponent extends EntityManagerComponent {
  private static final FloatingDemonConfig FLOATING_DEMON_CONFIG =
      FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json").floatingDemon;
  private final int numberOfBombEnemies = new Random().nextInt(10, 20);
  private int numEnemies = 0;

  @Override
  public void create() {
    entity.getEvents().addListener("RoomCreated", this::spawnEnemies);
  }

  public void spawnEnemies(Entity target) {
    spawnBombEnemies(target);
    spawnFloatingDemons(target);
  }

  /** Creates ghosts at random valid tiles and sets the player as their target. */
  public void spawnBombEnemies(Entity target) {
    GridPoint2 maxPosition = spawnableArea();
    for (int i = 0; i < numberOfBombEnemies; i++) {
      GridPoint2 position = RandomUtils.random(new GridPoint2(0, 0), maxPosition);
      Entity bombEnemy = NPCFactory.createBombEnemy(target);
      track(bombEnemy);
      spawnEntityAt(bombEnemy, position, true, true);
    }
  }

  /** Creates floating demons at random positions with local triangle patrol paths. */
  private void spawnFloatingDemons(Entity target) {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    float mapWidth = terrain.getMapBounds(0).x * terrain.getTileSize();
    float mapHeight = terrain.getMapBounds(0).y * terrain.getTileSize();
    GridPoint2 maxPosition = spawnableArea();

    for (int i = 0; i < FLOATING_DEMON_CONFIG.spawnCount; i++) {
      GridPoint2 tilePosition = RandomUtils.random(new GridPoint2(1, 1), maxPosition);
      Vector2 spawnPosition = terrain.tileToWorldPosition(tilePosition);

      float leftX = Math.max(1f, spawnPosition.x - FLOATING_DEMON_CONFIG.patrolRange);
      float rightX = Math.min(mapWidth - 1f, spawnPosition.x + FLOATING_DEMON_CONFIG.patrolRange);
      float topY = Math.min(mapHeight - 1f, spawnPosition.y + FLOATING_DEMON_CONFIG.patrolHeight);

      Vector2 leftPoint = new Vector2(leftX, spawnPosition.y);
      Vector2 topPoint = new Vector2(spawnPosition.x, topY);
      Vector2 rightPoint = new Vector2(rightX, spawnPosition.y);

      Entity demon = NPCFactory.createFloatingDemon(target, leftPoint, topPoint, rightPoint);

      track(demon);
      spawnEntityAt(demon, tilePosition, true, true);
    }
  }

  public void spawnSplitEnemy(Entity target) {
    GridPoint2 maxPosition = spawnableArea();
    GridPoint2 position = RandomUtils.random(new GridPoint2(0, 0), maxPosition);

    Entity splitEnemy = NPCFactory.createBombEnemy(target);
    splitEnemy.addComponent(new SplitComponent(target));

    track(splitEnemy);
    splitEnemy.getEvents().addListener("spawnChildren", this::enemyTriggerSpawn);

    spawnEntityAt(splitEnemy, position, true, true);
  }

  /**
   * Tracks an enemy by incrementing numEnemies and listening for its death.
   *
   * @param enemy The enemy being tracked.
   */
  void track(Entity enemy) {
    numEnemies++;
    enemy.getEvents().addListener("entityDied", this::onEnemyDefeated);
  }

  /** Decreases numEnemies and triggers roomCleared when all enemies are dead. */
  private void onEnemyDefeated() {
    numEnemies--;
    if (numEnemies <= 0) {
      entity.getEvents().trigger("roomCleared");
    }
  }

  /**
   * Callback function for when an enemy wishes to spawn another enemy.
   *
   * @param newEnemy the new enemy should not already be registered
   */
  private void enemyTriggerSpawn(Entity newEnemy) {
    track(newEnemy);
    spawnEntity(newEnemy);
  }
}
