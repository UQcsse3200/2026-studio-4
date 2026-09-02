package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.rooms.EnemyManagerComponent;
import com.csse3200.game.components.rooms.ExitComponent;
import com.csse3200.game.components.rooms.ObstacleComponent;
import com.csse3200.game.components.rooms.RoomAssetsComponent;
import com.csse3200.game.components.rooms.WallComponent;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.entities.Entity;

/** Factory for creating rooms with their terrain and gameplay components. */
public class RoomFactory {
  private RoomFactory() {
    throw new IllegalStateException("Instantiating static utility class");
  }

  /** Creates a room entity from its declarative definition. */
  public static Entity createRoom(RoomConfig room, CameraComponent camera, boolean cleared) {
    TerrainFactory terrainFactory = new TerrainFactory(camera);
    return new Entity()
        .addComponent(new RoomAssetsComponent())
        .addComponent(new GameAreaDisplay(room.title))
        .addComponent(
            terrainFactory.createDungeonTerrain(new GridPoint2(room.mapWidth, room.mapHeight)))
        .addComponent(new WallComponent())
        .addComponent(new ObstacleComponent(room.obstacles))
        .addComponent(new ExitComponent(room.exits))
        .addComponent(
            new EnemyManagerComponent(cleared ? new EnemySpawnConfig[0] : room.enemySpawns));
  }
}
