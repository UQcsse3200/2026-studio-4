package com.csse3200.game.components.rooms.configs;

import java.util.Objects;

/** Declarative definition of one room in the world graph. */
public class RoomConfig {
  public int mapWidth = 50;
  public int mapHeight = 50;
  public String id;
  public String dungeonId;
  public String title;
  public EntryPointConfig[] entryPoints = new EntryPointConfig[0];
  public EnemySpawnConfig[] enemySpawns = new EnemySpawnConfig[0];
  public ObstacleConfig[] obstacles = new ObstacleConfig[0];
  public ExitConfig[] exits = new ExitConfig[0];

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
