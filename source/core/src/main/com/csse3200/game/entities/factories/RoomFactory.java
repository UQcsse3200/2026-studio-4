package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.rooms.*;
import com.csse3200.game.components.rooms.EnemyManagerComponent;
import com.csse3200.game.components.rooms.ExitComponent;
import com.csse3200.game.components.rooms.ObstacleComponent;
import com.csse3200.game.components.rooms.TrapManagerComponent;
import com.csse3200.game.components.rooms.WallComponent;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.entities.Entity;
import java.util.List;

/** Factory for creating rooms with their terrain and gameplay components. */
public class RoomFactory {
  private RoomFactory() {
    throw new IllegalStateException("Instantiating static utility class");
  }

  /** Creates a room entity from its declarative definition. */
  public static Entity createRoom(RoomConfig room, CameraComponent camera, boolean cleared) {
    GridPoint2 mapSize = new GridPoint2(room.mapWidth, room.mapHeight);
    TerrainFactory terrainFactory = new TerrainFactory(camera, mapSize);
    ObstacleComponent obstacle = new ObstacleComponent(room);
    spawnMap(room, terrainFactory, obstacle);
    return new Entity()
        .addComponent(new GameAreaDisplay(room.title))
        .addComponent(terrainFactory.getTerrain())
        .addComponent(new WallComponent())
        .addComponent(new FollowingCameraComponent())
        .addComponent(obstacle)
        .addComponent(new TrapManagerComponent(room.trapSpawns))
        .addComponent(new ExitComponent(room.exits))
        .addComponent(
            new EnemyManagerComponent(
                cleared ? new EnemySpawnConfig[0] : room.enemySpawns, camera));
  }

  public static void spawnMap(RoomConfig room, TerrainFactory terrain, ObstacleComponent obstacle) {
    // Gets the list of obstacles from the spawnConfig
    List<String> spawnConfig = room.obstacles.spawns;
    // Gets the height and width of the room
    int height = room.mapHeight;
    int width = room.mapWidth;
    // Loops through the spawn config
    for (int y = 0; y < spawnConfig.size(); y++) {
      String row = spawnConfig.get(y);
      for (int x = 0; x < row.length(); x++) {
        char spawnType = row.charAt(x);
        terrain.tile(x, y, spawnType);
      }
    }
  }
}
