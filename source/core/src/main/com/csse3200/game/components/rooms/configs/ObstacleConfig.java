package com.csse3200.game.components.rooms.configs;

/** A fixed obstacle in a room. Supported types are ROCK and BARREL. */
public class ObstacleConfig extends PositionConfig {
  public ObstacleType type = ObstacleType.ROCK;

  public enum ObstacleType {
    ROCK,
    BARREL
  }
}
