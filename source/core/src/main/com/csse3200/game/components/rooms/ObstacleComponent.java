package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.utils.math.RandomUtils;
import java.util.Random;

/** Spawns rocks and holes at random safe positions in a room. */
public class ObstacleComponent extends EntityManagerComponent {
  private static final int MINIMUM_OBSTACLES = 10;
  private static final int MAXIMUM_OBSTACLES = 20;

  private final int numberOfObstacles = new Random().nextInt(MINIMUM_OBSTACLES, MAXIMUM_OBSTACLES);

  @Override
  public void create() {
    spawnObstacles(true);
    spawnObstacles(false);
  }

  private void spawnObstacles(boolean rock) {
    GridPoint2 maxPosition = spawnableArea();
    for (int i = 0; i < numberOfObstacles; i++) {
      GridPoint2 position = RandomUtils.random(new GridPoint2(0, 0), maxPosition);
      Entity obstacle = rock ? ObstacleFactory.createRock() : ObstacleFactory.createHole();
      spawnEntityAt(obstacle, position, true, false);
    }
  }
}
