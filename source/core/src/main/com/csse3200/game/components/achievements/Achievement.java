package com.csse3200.game.components.achievements;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;

public interface Achievement {
  /** Returns true if this event just unlocked the achievement. */
  boolean onEnemyDied(EnemyType killedType);

  void onPlayerDamaged();

  boolean isUnlocked();

  String getName();
}
