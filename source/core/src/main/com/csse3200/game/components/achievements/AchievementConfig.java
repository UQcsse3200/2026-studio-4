package com.csse3200.game.components.achievements;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import java.util.List;

public class AchievementConfig {
  public String type; // discriminator, matches a case in the factory
  public String name;
  public float target;
  public EnemyType enemyType;
  public List<EnemyType> enemyTypes;
  public String dungeonId;
  public float maxSeconds;
}
