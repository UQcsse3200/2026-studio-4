package com.csse3200.game.components.achievements;

/** Unlocks the first time a specific dungeon is completed. */
public class DungeonClearAchievement extends Achievement {
  private final String dungeonId;

  public DungeonClearAchievement(String dungeonId, String name) {
    super(name);
    this.dungeonId = dungeonId;
  }

  @Override
  public boolean onDungeonCompleted(String completedId) {
    return dungeonId.equals(completedId) && unlock();
  }
}
