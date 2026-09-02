package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.utils.math.RandomUtils;
import java.util.Random;

/** Spawns rocks and barrels at random safe positions in a room. */
public class ObstacleComponent extends EntityManagerComponent {
  private static final int MINIMUM_OBSTACLES = 10;
  private static final int MAXIMUM_OBSTACLES = 20;
  private static final GridPoint2 MINIMUM_POSITION = new GridPoint2(1, 1);

  private final int numberOfObstacles = new Random().nextInt(MINIMUM_OBSTACLES, MAXIMUM_OBSTACLES);

  @Override
  public void create() {
    spawnObstacles(true);
    spawnObstacles(false);
  }

  private void spawnObstacles(boolean rock) {
    spawnableArea()
        .ifPresent(
            area -> {
              GridPoint2 maxPosition = area.sub(1, 1);
              for (int i = 0; i < numberOfObstacles; i++) {
                GridPoint2 position = RandomUtils.random(MINIMUM_POSITION, maxPosition);
                Entity obstacle =
                    rock ? ObstacleFactory.createRock() : ObstacleFactory.createBarrel();
                spawnEntityAt(obstacle, position, true, false);
              }
            });
  }
}
