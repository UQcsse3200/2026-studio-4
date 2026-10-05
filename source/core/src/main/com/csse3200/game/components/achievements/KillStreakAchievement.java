package com.csse3200.game.components.achievements;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;

/** Unlocks after killing `target` enemies in a row without the player taking damage. */
public class KillStreakAchievement implements Achievement {
  private final int target;
  private final String name;
  private int streak = 0;
  private boolean unlocked = false;

  public KillStreakAchievement(int target, String name) {
    this.target = target;
    this.name = name;
  }

  public boolean onEnemyDied(EnemyType killedType) {
    if (unlocked) {
      return false;
    }
    streak++;
    if (streak >= target) {
      unlocked = true;
      return true;
    }
    return false;
  }

  public void onPlayerDamaged() {
    //        streak = 0;
  }

  public boolean isUnlocked() {
    return unlocked;
  }

  public String getName() {
    return name;
  }

  public int getStreak() {
    return streak;
  }
}
