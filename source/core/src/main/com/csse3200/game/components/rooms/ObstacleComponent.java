package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.utils.math.RandomUtils;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component used to create obstacles and add them to game area entities. Any game ares which
 * utilises obstacles such as walls or rocks should have an instance of this class registered. This
 * class can be extended for more specific obstacle needs.
 */
public class ObstacleComponent extends EntityManagerComponent {
  private static final Logger logger = LoggerFactory.getLogger(ObstacleComponent.class);
  private int NumOfObstacles = new Random().nextInt(10, 20);
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
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    if (terrain == null) {
      logger.error("Spawning obstacle on an entity without a terrain");
      return;
    }

    GridPoint2 minPos = new GridPoint2(0, 0);
    GridPoint2 maxPos = terrain.getMapBounds(0).sub(2, 2);

    for (int i = 0; i < NumOfObstacles; i++) {
      GridPoint2 randomPos = RandomUtils.random(minPos, maxPos);
      Entity createdObstacle = ObstacleFactory.createObstacle(obstacle);
      spawnEntityAt(createdObstacle, randomPos, true, false);
    }
  }


}
