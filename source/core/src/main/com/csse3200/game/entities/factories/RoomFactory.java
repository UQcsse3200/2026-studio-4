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

/** Factory for a basic room entity with terrain and a title. */
public class RoomFactory {
  private RoomFactory() {
    throw new IllegalStateException("Instantiating static utility class");
  }

  /** Creates one basic, playable room. */
  public static Entity createRoom(String roomName, CameraComponent camera) {
    TerrainConfig terrainConfig = new TerrainConfig();
    terrainConfig.alternateTextureOneCount = 100;
    terrainConfig.alternateTextureTwoCount = 80;

    RoomAssetsComponent assets = new RoomAssetsComponent();
    assets.setTerrainConfig(terrainConfig);
    TerrainFactory terrainFactory = new TerrainFactory(camera);
    return new Entity()
        .addComponent(assets)
        .addComponent(new GameAreaDisplay(roomName))
        .addComponent(terrainFactory.createTerrain(terrainConfig))
        .addComponent(new WallComponent())
        .addComponent(new ObstacleComponent())
        .addComponent(new EnemyManagerComponent());
  }
}
