package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.utils.math.RandomUtils;
import java.util.Random;

/**
 * Component used to create enemies and add them to game area entities. Any game areas which
 * utilises enemies such as ghosts or bombers should have an instance of this class registered. This
 * class can be extended for more specific enemy needs.
 */
public class EnemyManagerComponent extends EntityManagerComponent {
  private final int NUM_GHOSTS = new Random().nextInt(10, 20);
  private int numEnemies = 0;

  public EnemyManagerComponent() {
    super();
  }

  @Override
  public void create() {
    entity.getEvents().addListener("RoomCreated", this::spawnGhosts);
  }

  /**
   * Spawns ghosts with the given target.
   *
   * @param target The target for the ghosts to attack.
   */
  public void spawnGhosts(Entity target) {
    GridPoint2 maxPos = this.spawnableArea();
    for (int i = 0; i < NUM_GHOSTS; i++) {
      GridPoint2 randomPos = RandomUtils.random(new GridPoint2(0, 0), maxPos);
      Entity ghost = NPCFactory.createGhost(target);
      track(ghost);
      spawnEntityAt(ghost, randomPos, true, true);
    }
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

  /**
   * Decreases numEnemies and triggers roomCleared when all enemies are dead
   *
   */
  private void onEnemyDefeated() {
    numEnemies--;
    if (numEnemies <= 0) {
      entity.getEvents().trigger("roomCleared");
    }
  }
}
