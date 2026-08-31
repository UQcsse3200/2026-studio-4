package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
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
    entity.getEvents().addListener("RoomCreated", this::spawnBombEnemies);
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

  /**
   * Tracks an enemy by incrementing numEnemies and listening for its death.
   *
   * @param enemy The enemy being tracked.
   */
  void track(Entity enemy) { // set to not private for testing reasons
    numEnemies++;
    enemy.getEvents().addListener("entityDied", this::onEnemyDefeated);
  }

  /** Decreases numEnemies and triggers roomCleared when all enemies are dead.*/
  private void onEnemyDefeated() {
    numEnemies--;
    if (numEnemies <= 0) {
      entity.getEvents().trigger("roomCleared");
    }
  }
}
