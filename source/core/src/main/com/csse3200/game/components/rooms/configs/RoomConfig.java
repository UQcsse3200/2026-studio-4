package com.csse3200.game.components.rooms.configs;

import java.util.Objects;

/** Declarative definition of one room in the world graph. */
public class RoomConfig {
  public int mapWidth = 50;
  public int mapHeight = 50;
  public String id;
  public String dungeonId;
  public String title;

  /**
   * Visual theme. {@code null} (the default) builds a dungeon room from its tileset; {@code
   * "library"} builds a home-library room with floor/wall characters and {@link #props}.
   */
  public String theme;

  /** Tileset for the floor and walls, overriding the one chosen from {@link #dungeonId}. */
  public String tileset;
  public EntryPointConfig[] entryPoints = new EntryPointConfig[0];
  public EnemySpawnConfig[] enemySpawns = new EnemySpawnConfig[0];
  public NpcSpawnConfig[] npcSpawns = new NpcSpawnConfig[0];
  public TrapSpawnConfig[] trapSpawns = new TrapSpawnConfig[0];
  public ExitConfig[] exits = new ExitConfig[0];
  public RoomSpawnConfig obstacles = new RoomSpawnConfig();

  /** Furniture and decorations for themed rooms. Ignored by dungeon rooms. */
  public PropConfig[] props = new PropConfig[0];

  /**
   * How dark unlit parts of the room are, 0 (normal) to 1 (black). Above 0 the room is lit only
   * by its light-giving props, its {@link #lights} and the player's lantern.
   */
  public float darkness;

  /** Colour of the dark, as hex. A little blue reads as night. */
  public String shadowColor = "#05040c";

  /** Extra lights not tied to a prop, e.g. moonlight through a window. */
  public LightConfig[] lights = new LightConfig[0];

  /** Finds a named entry point, or null if the definition has none. */
  public EntryPointConfig getEntryPoint(String entryPointId) {
    for (EntryPointConfig entryPoint : entryPoints) {
      if (Objects.equals(entryPoint.id, entryPointId)) {
        return entryPoint;
      }
    }
    return null;
  }

  /** Finds a named exit, or null if the definition has none. */
  public ExitConfig getExit(String exitId) {
    for (ExitConfig exit : exits) {
      if (Objects.equals(exit.id, exitId)) {
        return exit;
      }
    }
    return null;
  }
}
