package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.utils.math.GridPoint2Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Spawns invisible physics walls around a room terrain. */
public class WallComponent extends EntityManagerComponent {
  private static final Logger logger = LoggerFactory.getLogger(WallComponent.class);
  private static final float WALL_WIDTH = 0.1f;
  private Vector2 WALLBOUND;

  public Vector2 getWallBounds() {
    return WALLBOUND;
  }

  @Override
  public void create() {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    if (terrain == null) {
      logger.error("Cannot spawn walls in a room without terrain");
      return;
    }

    float tileSize = terrain.getTileSize();
    GridPoint2 tileBounds = terrain.getMapBounds(0);
    WALLBOUND = new Vector2(tileBounds.x * tileSize, tileBounds.y * tileSize);

    spawnEntityAt(
        ObstacleFactory.createWall(WALL_WIDTH, WALLBOUND.y), GridPoint2Utils.ZERO, false, false);
    spawnEntityAt(
        ObstacleFactory.createWall(WALL_WIDTH, WALLBOUND.y),
        new GridPoint2(tileBounds.x, 0),
        false,
        false);
    spawnEntityAt(
        ObstacleFactory.createWall(WALLBOUND.x, WALL_WIDTH),
        new GridPoint2(0, tileBounds.y),
        false,
        false);
    spawnEntityAt(
        ObstacleFactory.createWall(WALLBOUND.x, WALL_WIDTH), GridPoint2Utils.ZERO, false, false);
  }
}
