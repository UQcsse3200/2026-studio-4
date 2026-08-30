package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.utils.math.RandomUtils;
import java.util.Random;

/** Spawns a random group of ghosts when its room is started. */
public class EnemyManagerComponent extends EntityManagerComponent {
  private final int numberOfGhosts = new Random().nextInt(10, 20);

  @Override
  public void create() {
    entity.getEvents().addListener("RoomCreated", this::spawnGhosts);
  }

  /** Creates ghosts at random valid tiles and sets the player as their target. */
  public void spawnGhosts(Entity target) {
    GridPoint2 maxPosition = spawnableArea();
    for (int i = 0; i < numberOfGhosts; i++) {
      GridPoint2 position = RandomUtils.random(new GridPoint2(0, 0), maxPosition);
      Entity ghost = NPCFactory.createGhost(target);
      spawnEntityAt(ghost, position, true, true);
    }
  }
}
