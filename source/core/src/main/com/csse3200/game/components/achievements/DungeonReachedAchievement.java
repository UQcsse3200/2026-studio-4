package com.csse3200.game.components.achievements;

/** Unlocks the first time a specific dungeon is entered. */
public class DungeonReachedAchievement extends Achievement {
  private final String dungeonId;

  public DungeonReachedAchievement(String dungeonId, String name) {
    super(name);
    this.dungeonId = dungeonId;
  }

  @Override
  public boolean onDungeonEntered(String enteredId) {
    return dungeonId.equals(enteredId) && unlock();
  }
}
