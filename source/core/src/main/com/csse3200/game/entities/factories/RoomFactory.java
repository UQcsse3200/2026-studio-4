package com.csse3200.game.entities.factories;

import com.csse3200.game.areas.terrain.TerrainConfig;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.rooms.EnemyManagerComponent;
import com.csse3200.game.components.rooms.ObstacleComponent;
import com.csse3200.game.components.rooms.RoomAssetsComponent;
import com.csse3200.game.components.rooms.WallComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.Renderer;

/** Factory to create rooms with predefined components */
public class RoomFactory {
  private Renderer renderer;

  public RoomFactory(Renderer renderer) {
    this.renderer = renderer;
  }

  public Entity createRoom(String roomName) {
    return createRoom(renderer, roomName);
  }


  public static Entity createRoom(Renderer renderer, String roomName) {
    TerrainFactory terrainFactory = new TerrainFactory(renderer.getCamera());
    TerrainConfig terrainConfig = new TerrainConfig();
    terrainConfig.gAltTexture1Count = 100;
    terrainConfig.gAltTexture2Count = 80;

    RoomAssetsComponent assetsComponent = new RoomAssetsComponent();
    assetsComponent.setTerrainConfig(terrainConfig);

    Entity entity =
        new Entity()
            .addComponent(assetsComponent)
            .addComponent(new GameAreaDisplay(roomName))
            .addComponent(terrainFactory.createTerrain(terrainConfig, renderer.getCamera()))
            .addComponent(new WallComponent())
            .addComponent(new ObstacleComponent())
                .addComponent(new EnemyManagerComponent());
    return entity;
  }
}
