package com.csse3200.game.components.achievements;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;

/** Unlocks after killing `target` enemies in a row without the player taking damage. */
public class KillStreakAchievement extends Achievement {
  private final int target;
  private int streak = 0;

  public KillStreakAchievement(int target, String name) {
    super(name);
    this.target = target;
  }

  @Override
  public boolean onEnemyDied(EnemyType killedType) {
    if (isUnlocked()) {
      return false;
    }
    streak++;
    return streak >= target && unlock();
  }

  @Override
  public boolean onPlayerDamaged() {
    streak = 0;
    return false;
  }

  public int getStreak() {
    return streak;
  }
}
