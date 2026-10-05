package com.csse3200.game.components.rooms.configs;

/** An enemy type and its fixed spawn position within a room. */
public class EnemySpawnConfig extends PositionConfig {
  public EnemyType type = EnemyType.BEETLE;

  public enum EnemyType {
    CRAB,
    HARPY,
    MUMMY,
    WASP,
    BEETLE,
    WOLF,
    CROW,
    JINGWEI,
    ZOMBIE,
    WUKONG,
    LONGWEI,
    BUG,
    THOR,
    DARK_ELVES,
    JOTUNN,
    CYCLOPS,
    GOLEM,
    MEDUSA,
    CERBERUS,
    DRAGON,
    FINAL_BOSS,
    SNAKE_MINI_BOSS
  }
}
