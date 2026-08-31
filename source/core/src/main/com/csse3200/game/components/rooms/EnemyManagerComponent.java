package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.components.SpiltComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.utils.math.RandomUtils;
import java.util.Random;

/** Spawns a random group of ghosts when its room is started. */
public class EnemyManagerComponent extends EntityManagerComponent {
  private final int numberOfBombEnemies = new Random().nextInt(10, 20);
  private int numEnemies = 0;

  @Override
  public void create() {
    entity.getEvents().addListener("RoomCreated", this::spawnEnemies);
  }

  public void spawnEnemies(Entity target) {
    // spawnBombEnemies(target);
    spawnSplitEnemy(target);
  }

  /** Creates ghosts at random valid tiles and sets the player as their target. */
  public void spawnBombEnemies(Entity target) {
    GridPoint2 maxPosition = spawnableArea();
    for (int i = 0; i < numberOfBombEnemies; i++) {
      GridPoint2 position = RandomUtils.random(new GridPoint2(0, 0), maxPosition);
      Entity ghost = NPCFactory.createBombEnemy(target);
      track(ghost);
      spawnEntityAt(ghost, position, true, true);
    }
  }

  public void spawnSplitEnemy(Entity target) {
    GridPoint2 maxPosition = spawnableArea();
    GridPoint2 position = RandomUtils.random(new GridPoint2(0, 0), maxPosition);

    Entity splitEnemy = NPCFactory.createBombEnemy(target);
    splitEnemy.addComponent(new SpiltComponent(target));

    track(splitEnemy);
    splitEnemy.getEvents().addListener("spawnChildren", this::enemyTriggerSpawn);

    spawnEntityAt(splitEnemy, position, true, true);
  }

  /**
   * Tracks an enemy by incrementing numEnemies and listening for its death.
   *
   * @param enemy The enemy being tracked.
   */
  void track(Entity enemy) { // set to not private for testing reasons
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
   * Callback function for when an enemy wishes to spawn another enemy
   *
   * <p>Provide this to a listener to give an enemy the ability to add new entities to game. A spawn
   * should be triggered BEFORE an enemy triggers its death to prevent numEnemies <= 0.
   *
   * @param newEnemy the new enemy should not be registed.
   */
  private void enemyTriggerSpawn(Entity newEnemy) {
    numEnemies++;
    spawnEntity(newEnemy);
  }
}
