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
  public NpcSpawnConfig[] npcSpawns = new NpcSpawnConfig[0];
  public TrapSpawnConfig[] trapSpawns = new TrapSpawnConfig[0];
  public PropSpawnConfig[] propSpawns = new PropSpawnConfig[0];
  public ExitConfig[] exits = new ExitConfig[0];
  public RoomSpawnConfig obstacles = new RoomSpawnConfig();

  /**
   * Whether this room is a safe practice space. While the player is in it their weapons count as
   * upgraded, so heavy attacks can be tried, and anything lent to them by its supply stands is
   * taken back when they leave.
   */
  public boolean trainingGrounds;

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
