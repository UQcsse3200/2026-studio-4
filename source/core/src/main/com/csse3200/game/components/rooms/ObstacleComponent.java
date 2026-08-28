package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.utils.math.RandomUtils;
import java.util.Random;

/**
 * Component used to create obstacles and add them to game area entities. Any game ares which
 * utilises obstacles such as walls or rocks should have an instance of this class registered. This
 * class can be extended for more specific obstacle needs.
 */
public class ObstacleComponent extends EntityManagerComponent {
  private final int NumOfObstacles = new Random().nextInt(10, 20);
  private static final String TREE = "Tree";
  private static final String ROCK = "Rock";
  private static final String HOLE = "Hole";

  public ObstacleComponent() {
    super();
  }

  public void create() {
    createObstacle(ROCK);
    createObstacle(HOLE);
  }

  public void createObstacle(String obstacle) {
    GridPoint2 maxPos = this.spawnableArea();
    for (int i = 0; i < NumOfObstacles; i++) {
      GridPoint2 randomPos = RandomUtils.random(new GridPoint2(0, 0), maxPos);
      Entity createdObstacle = ObstacleFactory.createObstacle(obstacle);
      spawnEntityAt(createdObstacle, randomPos, true, false);
    }
  }
}
