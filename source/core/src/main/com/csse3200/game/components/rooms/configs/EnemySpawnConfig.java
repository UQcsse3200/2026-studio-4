package com.csse3200.game.components.rooms.configs;

/** An enemy type and its fixed spawn position within a room. */
public class EnemySpawnConfig extends PositionConfig {
  public EnemyType type = EnemyType.BEETLE;

  /** Optional loot table path; omitted enemies use the room's table. */
  public String lootTable;

  public enum EnemyType {
    CRAB,
    HARPY,
    MUMMY,
    WASP,
    BEETLE,
    CYCLOPS,
    GOLEM,
    MEDUSA,
    CERBERUS,
    FINAL_BOSS,
    SNAKE_MINI_BOSS
  }
}
