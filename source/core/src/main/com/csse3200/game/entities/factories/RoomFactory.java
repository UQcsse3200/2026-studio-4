package com.csse3200.game.entities.factories;

import com.csse3200.game.areas.ForestGameArea;
import com.csse3200.game.areas.terrain.TerrainConfig;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.rooms.RoomAssetsComponent;
import com.csse3200.game.components.rooms.WallComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.Renderer;

/** Factory to create rooms with predefined components */
public class RoomFactory {

  /**
   * Creates the original forest game area as a entity.
   *
   * <p>Can be removed later.
   *
   * @param renderer
   * @return
   */
  public static Entity createForestGameArea(Renderer renderer) {
    TerrainFactory terrain = new TerrainFactory(renderer.getCamera());
    Entity entity = new Entity().addComponent(new ForestGameArea(terrain));
    return entity;
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
            .addComponent(new WallComponent());

    return entity;
  }
}
