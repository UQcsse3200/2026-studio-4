package com.csse3200.game.components.rooms.configs;

/** An enemy type and its fixed spawn position within a room. */
public class EnemySpawnConfig extends PositionConfig {
  public EnemyType type = EnemyType.BOMB;

  public enum EnemyType {
    BOMB,
    CHASE,
    FLOATING_DEMON
  }
}
