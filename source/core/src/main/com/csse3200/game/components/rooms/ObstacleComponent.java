package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.components.rooms.configs.ObstacleConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ObstacleFactory;

/** Spawns the fixed rocks and barrels declared for a room. */
public class ObstacleComponent extends EntityManagerComponent {
  private final ObstacleConfig[] obstacles;

  public ObstacleComponent(ObstacleConfig[] obstacles) {
    this.obstacles = obstacles;
  }

  @Override
  public void create() {
    for (ObstacleConfig obstacle : obstacles) {
      Entity entity;
      switch (obstacle.type) {
        case ROCK:
          entity = ObstacleFactory.createRock();
          break;
        case BARREL:
          entity = ObstacleFactory.createBarrel();
          break;
        default:
          throw new IllegalArgumentException("Unsupported obstacle type: " + obstacle.type);
      }
      spawnEntityAt(entity, new GridPoint2(obstacle.x, obstacle.y), true, false);
    }
  }
}
