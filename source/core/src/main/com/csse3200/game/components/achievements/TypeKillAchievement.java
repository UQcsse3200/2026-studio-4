package com.csse3200.game.components.achievements;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;

/** Unlocks after a target number of kills of one enemy type (or any type, if null). */
public class TypeKillAchievement implements Achievement {
  private final EnemyType type;
  private final int target;
  private final String name;
  private int count = 0;
  private boolean unlocked = false;

  public TypeKillAchievement(EnemyType type, int target, String name) {
    this.type = type;
    this.target = target;
    this.name = name;
  }

  /** Returns true if this kill just unlocked the achievement. */
  public boolean onEnemyDied(EnemyType killedType) {
    if (unlocked || (type != null && killedType != type)) {
      return false;
    }
    count++;
    if (count >= target) {
      unlocked = true;
      return true;
    }
    return false;
  }

  public void onPlayerDamaged() {
    // no-op; not a streak achievement
  }

  public boolean isUnlocked() {
    return unlocked;
  }

  public String getName() {
    return name;
  }

  public int getCount() {
    return count;
  }

  public int getTarget() {
    return target;
  }
}
