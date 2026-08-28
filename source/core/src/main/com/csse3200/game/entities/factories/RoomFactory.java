package com.csse3200.game.entities.factories;

import com.csse3200.game.areas.terrain.TerrainConfig;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.rooms.EnemyManagerComponent;
import com.csse3200.game.components.rooms.ObstacleComponent;
import com.csse3200.game.components.rooms.RoomAssetsComponent;
import com.csse3200.game.components.rooms.WallComponent;
import com.csse3200.game.entities.Entity;

/** Factory to create rooms with predefined components */
public class RoomFactory {

  /**
   * Creates a basic room entity with the given room name.
   *
   * @param renderer
   * @param roomName
   * @return
   */
  public static Entity createRoom(String roomName, CameraComponent camera) {
    TerrainFactory terrainFactory = new TerrainFactory(camera);
    TerrainConfig terrainConfig = new TerrainConfig();
    terrainConfig.gAltTexture1Count = 100;
    terrainConfig.gAltTexture2Count = 80;

    RoomAssetsComponent assetsComponent = new RoomAssetsComponent();
    assetsComponent.setTerrainConfig(terrainConfig);

    Entity entity =
        new Entity()
            .addComponent(assetsComponent)
            .addComponent(new GameAreaDisplay(roomName))
            .addComponent(terrainFactory.createTerrain(terrainConfig, camera))
            .addComponent(new WallComponent())
            .addComponent(new ObstacleComponent())
            .addComponent(new EnemyManagerComponent());
    return entity;
  }
}
