package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.utils.math.GridPoint2Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Spawns wall obstacles around a terrain. Requires entity to also have TerrainComponent */
public class WallComponent extends EntityManagerComponent {
  private static final Logger logger = LoggerFactory.getLogger(WallComponent.class);
  private static final float WALL_WIDTH = 0.1f;

  public WallComponent() {
    super();
  }

  @Override
  public void create() {
    spawnWalls();
  }

  private void spawnWalls() {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    if (terrain == null) {
      logger.error("Spawning walls on an entity without a terrain");
      return;
    }

    float tileSize = terrain.getTileSize();
    GridPoint2 tileBounds = terrain.getMapBounds(0);
    Vector2 worldBounds = new Vector2(tileBounds.x * tileSize, tileBounds.y * tileSize);

    // Left
    spawnEntityAt(
        ObstacleFactory.createWall(WALL_WIDTH, worldBounds.y), GridPoint2Utils.ZERO, false, false);
    // Right
    spawnEntityAt(
        ObstacleFactory.createWall(WALL_WIDTH, worldBounds.y),
        new GridPoint2(tileBounds.x, 0),
        false,
        false);
    // Top
    spawnEntityAt(
        ObstacleFactory.createWall(worldBounds.x, WALL_WIDTH),
        new GridPoint2(0, tileBounds.y),
        false,
        false);
    // Bottom
    spawnEntityAt(
        ObstacleFactory.createWall(worldBounds.x, WALL_WIDTH), GridPoint2Utils.ZERO, false, false);
  }
}
