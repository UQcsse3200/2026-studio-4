package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TerrainBuilder;
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
import com.csse3200.game.entities.configs.InteractableNpcConfigs;
import com.csse3200.game.files.FileLoader;
import java.util.List;

/** Factory for creating rooms with their terrain and gameplay components. */
public class RoomFactory {
  private static InteractableNpcConfigs friendlyNpcs;

  private RoomFactory() {
    throw new IllegalStateException("Instantiating static utility class");
  }

  /**
   * Creates a room from its declarative definition, camera component and the boolean value of its
   * cleared state.
   *
   * @param room The rooms declarative definition.
   * @param camera The camera for the room.
   * @param cleared Whether the room has been cleared.
   * @param tileset The tileset location
   * @return The room entity.
   */
  public static Entity createRoom(
      RoomConfig room, CameraComponent camera, boolean cleared, String tileset) {
    GridPoint2 mapSize = new GridPoint2(room.mapWidth, room.mapHeight);
    TerrainBuilder terrainBuilder = new TerrainBuilder(camera, mapSize, tileset);
    spawnMap(room, terrainBuilder);
    return new Entity()
        .addComponent(new GameAreaDisplay(room.title))
        .addComponent(terrainBuilder.getTerrain())
        .addComponent(new WallComponent())
        .addComponent(new FollowingCameraComponent())
        .addComponent(new ObstacleComponent(room, tileset))
        .addComponent(new TrapManagerComponent(room.trapSpawns))
        .addComponent(new ExitComponent(room.exits))
        .addComponent(
             new EnemyManagerComponent(
                 cleared ? new EnemySpawnConfig[0] : room.enemySpawns, room.id, camera))
        .addComponent(new FriendlyNpcManagerComponent(room.npcSpawns, getFriendlyNpcs()));
  }

  private static InteractableNpcConfigs getFriendlyNpcs() {
    if (friendlyNpcs == null) {
      InteractableNpcConfigs loaded =
          FileLoader.readClass(InteractableNpcConfigs.class, InteractableNpcConfigs.CONFIG_PATH);
      if (loaded == null) {
        throw new IllegalStateException("Unable to load " + InteractableNpcConfigs.CONFIG_PATH);
      }
      loaded.validate();
      friendlyNpcs = loaded;
    }
    return friendlyNpcs;
  }

  /**
   * Spawns the terrain of the map based upon the room config definition.
   *
   * @param room The room config.
   * @param terrain The TerrainBuilder.
   */
  public static void spawnMap(RoomConfig room, TerrainBuilder terrain) {
    // Gets the list of obstacles from the spawnConfig
    List<String> spawnConfig = room.obstacles.spawns;
    // Gets the height and width of the room
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
