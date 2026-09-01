package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base component for creating entities owned by a room. */
public abstract class EntityManagerComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(EntityManagerComponent.class);

  private final List<Entity> entities = new ArrayList<>();

  @Override
  public void dispose() {
    for (Entity spawnedEntity : entities) {
      spawnedEntity.dispose();
    }
  }

  /** Registers and records an entity that belongs to this room. */
  protected void spawnEntity(Entity spawnedEntity) {
    entities.add(spawnedEntity);
    ServiceLocator.getEntityService().register(spawnedEntity);
  }

  /** Places, registers, and records an entity at a terrain tile. */
  protected void spawnEntityAt(
      Entity spawnedEntity, GridPoint2 tilePosition, boolean centerX, boolean centerY) {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    if (terrain == null) {
      logger.error("Cannot spawn an entity in a room without terrain");
      return;
    }
    Vector2 worldPosition = terrain.tileToWorldPosition(tilePosition);
    float tileSize = terrain.getTileSize();
    if (centerX) {
      worldPosition.x += (tileSize / 2) - spawnedEntity.getCenterPosition().x;
    }
    if (centerY) {
      worldPosition.y += (tileSize / 2) - spawnedEntity.getCenterPosition().y;
    }
    spawnedEntity.setPosition(worldPosition);
    spawnEntity(spawnedEntity);
  }

  /** Returns the largest safe tile position for random room content. */
  protected Optional<GridPoint2> spawnableArea() {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    if (terrain == null) {
      logger.error("Cannot find a spawnable area in a room without terrain");
      return Optional.empty();
    }
    return Optional.of(terrain.getMapBounds(0).sub(2, 2));
  }
}
