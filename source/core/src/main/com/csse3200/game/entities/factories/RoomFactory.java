package com.csse3200.game.entities.factories;

import com.csse3200.game.areas.ForestGameArea;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.Renderer;

public class RoomFactory {
  public static Entity createForestGameArea(Renderer renderer) {
    TerrainFactory terrain = new TerrainFactory(renderer.getCamera());
    Entity entity = new Entity().addComponent(new ForestGameArea(terrain));
    return entity;
  }
}
