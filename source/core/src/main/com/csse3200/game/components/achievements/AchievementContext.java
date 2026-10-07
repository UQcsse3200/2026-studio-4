package com.csse3200.game.components.achievements;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;

/** Carries whatever happened in the game that achievements might care about. */
public class AchievementContext {
  public EnemyType enemyKilled;
  public boolean playerDamaged;
  public String dungeonCompletedId;
  public String dungeonEnteredId;
  public String dungeonId;
  public float dungeonSeconds;
  public Integer goldTotal;
  public Integer singleHitDamage;
}
