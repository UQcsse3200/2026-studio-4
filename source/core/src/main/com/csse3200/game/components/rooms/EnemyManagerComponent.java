package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.utils.math.RandomUtils;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component used to create obstacles and add them to game area entities. Any game ares which
 * utilises obstacles such as walls or rocks should have an instance of this class registered. This
 * class can be extended for more specific obstacle needs.
 */
public class EnemyManagerComponent extends EntityManagerComponent {
  private static final Logger logger = LoggerFactory.getLogger(EntityManagerComponent.class);
  private int NUM_GHOSTS = new Random().nextInt(10, 20);

  public EnemyManagerComponent() {
    super();
  }

  public void spawnGhosts(Entity target) {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    if (terrain == null) {
      logger.error("Spawning obstacle on an entity without a terrain");
      return;
    }
    GridPoint2 minPos = new GridPoint2(0, 0);
    GridPoint2 maxPos = terrain.getMapBounds(0).sub(2, 2);
    for (int i = 0; i < NUM_GHOSTS; i++) {
      GridPoint2 randomPos = RandomUtils.random(minPos, maxPos);
      Entity ghost = NPCFactory.createGhost(target);
      spawnEntityAt(ghost, randomPos, true, true);
    }
  }
}
