package com.csse3200.game.components.achievements;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;

/** Unlocks after a target number of kills of one enemy type (or any type, if null). */
public class TypeKillAchievement extends Achievement {
  private final EnemyType type;
  private final int target;
  private int count = 0;

  public TypeKillAchievement(EnemyType type, int target, String name) {
    super(name);
    this.type = type;
    this.target = target;
  }

  @Override
  public boolean onEnemyDied(EnemyType killedType) {
    if (isUnlocked() || (type != null && killedType != type)) {
      return false;
    }
    count++;
    return count >= target && unlock();
  }

  public int getCount() {
    return count;
  }

  public int getTarget() {
    return target;
  }
}
