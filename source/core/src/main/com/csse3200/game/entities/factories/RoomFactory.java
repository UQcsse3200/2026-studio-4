package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.attic.AtticLighting;
import com.csse3200.game.areas.attic.AtticTerrain;
import com.csse3200.game.areas.terrain.TerrainBuilder;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.rooms.*;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.components.rooms.configs.PropSpawnConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.tutorial.TrainingLoadoutComponent;
import com.csse3200.game.components.tutorial.TutorialPropManagerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.InteractableNpcConfigs;
import com.csse3200.game.files.FileLoader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import com.csse3200.game.rendering.RoomLightingComponent;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Factory for creating rooms with their terrain and gameplay components. */
public class RoomFactory {
  private static final Logger logger = LoggerFactory.getLogger(RoomFactory.class);
  private static InteractableNpcConfigs friendlyNpcs;

  public static final String DEFAULT_TILESET = "images/dungeons/tileSet0.png";
  public static final String DUNGEON1_TILESET = "images/dungeons/tileSet1.png";
  public static final String DUNGEON2_TILESET = "images/dungeons/tileSet2.png";
  public static final String DUNGEON3_TILESET = "images/dungeons/tileSet3.png";
  public static final String DUNGEON4_TILESET = "images/dungeons/tileSet4.png";
  public static final String DUNGEON5_TILESET = "images/dungeons/tileSet5.png";

  /** {@link RoomConfig#theme} value for home-library rooms such as dungeon selection. */
  public static final String LIBRARY_THEME = "library";

  private RoomFactory() {
    throw new IllegalStateException("Instantiating static utility class");
  }

  /** The tileset for a room: its own override if it has one, otherwise its dungeon's. */
  public static String getTileSet(RoomConfig room) {
    return room.tileset != null ? room.tileset : getTileSet(room.dungeonId);
  }

  /** Get the tileset to use based it's dungeon. */
  public static String getTileSet(String dungeonId) {
    return switch (dungeonId) {
      case "dungeonOne" -> DUNGEON1_TILESET;
      case "dungeonTwo" -> DUNGEON2_TILESET;
      case "dungeonThree" -> DUNGEON3_TILESET;
      case "dungeonFour" -> DUNGEON4_TILESET;
      case "finalDungeon" -> DUNGEON5_TILESET;
      case null, default -> {
        String msg =
            dungeonId == null
                ? "Getting tileset of null dungeonId"
                : "No tileset for dungeonId: " + dungeonId;
        logger.debug(msg);
        yield DEFAULT_TILESET;
      }
    };
  }

  /**
   * Creates a room from its declarative definition, camera component and the boolean value of its
   * cleared state.
   *
   * @param room The rooms declarative definition.
   * @param camera The camera for the room.
   * @param cleared Whether the room has been cleared.
   * @return The room entity.
   */
  public static Entity createRoom(RoomConfig room, CameraComponent camera, boolean cleared) {
    if (isLibrary(room)) {
      return createLibraryRoom(room, camera);
    }

    GridPoint2 mapSize = new GridPoint2(room.mapWidth, room.mapHeight);
    String tileset = getTileSet(room);
    TerrainBuilder terrainBuilder = new TerrainBuilder(camera, mapSize, tileset);

    spawnMap(room, terrainBuilder);
    Entity entity = new Entity();
    // Optional extras for decorated rooms (e.g. the attic): furniture props and dark lighting.
    if (room.props.length > 0) {
      entity.addComponent(new AtticFurnitureComponent(room, false));
    }
    if (room.darkness > 0f) {
      entity.addComponent(createLighting(room));
    }
    return entity
        .addComponent(new GameAreaDisplay(room.title))
        .addComponent(terrainBuilder.getTerrain())
        .addComponent(new ExitComponent(room.exits))
        .addComponent(new WallComponent())
        .addComponent(new FollowingCameraComponent())
        .addComponent(new ObstacleComponent(room, tileset))
        .addComponent(new TrapManagerComponent(room.trapSpawns))
        .addComponent(
            new EnemyManagerComponent(
                cleared ? new EnemySpawnConfig[0] : room.enemySpawns, room, camera))
        .addComponent(new RoomDataComponent(room))
        .addComponent(new RoomAcheivements())
        .addComponent(new RoomMusicComponent())
        .addComponent(new FriendlyNpcManagerComponent(room.npcSpawns, getFriendlyNpcs()));
  }

  /**
   * Creates a training grounds room: an ordinary room with its practice props and the loadout that
   * enchants the player's weapons and takes back anything lent to them. Only rooms flagged {@link
   * RoomConfig#trainingGrounds} get these, so every other room is built exactly as before.
   *
   * @param room The rooms declarative definition.
   * @param camera The camera for the room.
   * @param cleared Whether the room has been cleared.
   * @return The room entity.
   */
  public static Entity createTrainingRoom(
      RoomConfig room, CameraComponent camera, boolean cleared) {
    return createRoom(room, camera, cleared)
        .addComponent(new TutorialPropManagerComponent(room.propSpawns))
        .addComponent(new TrainingLoadoutComponent(supplyItemIds(room)));
  }

  /** The consumables a room's supply stands lend out. */
  private static Set<String> supplyItemIds(RoomConfig room) {
    return Arrays.stream(room.propSpawns)
        .filter(prop -> prop.type == PropSpawnConfig.PropType.SUPPLY_STAND)
        .map(prop -> prop.itemId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /** Whether a room uses the home-library theme instead of a dungeon tileset. */
  public static boolean isLibrary(RoomConfig room) {
    return LIBRARY_THEME.equals(room.theme);
  }

  /**
   * Builds a home-library room: library floor and walls, furniture props, and exits drawn as their
   * configured sprites (the dungeon tomes). Gameplay components match a dungeon room.
   */
  private static Entity createLibraryRoom(RoomConfig room, CameraComponent camera) {
    Entity entity = new Entity();
    if (room.darkness > 0f) {
      entity.addComponent(createLighting(room));
    }
    return entity
        .addComponent(new GameAreaDisplay(room.title))
        .addComponent(
            AtticTerrain.build(room.obstacles.spawns, room.mapWidth, room.mapHeight, camera))
        .addComponent(new ExitComponent(room.exits))
        .addComponent(new WallComponent())
        .addComponent(new FollowingCameraComponent())
        .addComponent(new AtticFurnitureComponent(room))
        .addComponent(new TrapManagerComponent(room.trapSpawns))
        .addComponent(new EnemyManagerComponent(room.enemySpawns, room, camera))
        .addComponent(new RoomDataComponent(room))
        .addComponent(new RoomAcheivements())
        .addComponent(new RoomMusicComponent())
        .addComponent(new FriendlyNpcManagerComponent(room.npcSpawns, getFriendlyNpcs()));
  }

  /** Darkness over the room's walled area, lit by its candles, lamps, tomes and explicit lights. */
  private static RoomLightingComponent createLighting(RoomConfig room) {
    List<String> grid = room.obstacles.spawns;
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = -1;
    int maxY = -1;
    for (int y = 0; y < grid.size(); y++) {
      String row = grid.get(y);
      for (int x = 0; x < row.length(); x++) {
        if (row.charAt(x) != AtticTerrain.VOID) {
          minX = Math.min(minX, x);
          minY = Math.min(minY, y);
          maxX = Math.max(maxX, x);
          maxY = Math.max(maxY, y);
        }
      }
    }
    float tile = 0.5f;
    return new RoomLightingComponent(
        room.darkness,
        Color.valueOf(room.shadowColor),
        AtticLighting.lightsFor(room),
        new Vector2(minX * tile, minY * tile),
        new Vector2((maxX + 1) * tile, (maxY + 1) * tile));
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
