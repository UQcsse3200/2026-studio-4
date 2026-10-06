package com.csse3200.game.components.achievements;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import java.util.EnumSet;
import java.util.Set;

public class AchievementsFactory {

  public static Achievement build(AchievementConfig c) {
    return switch (c.type) {
      case "dungeonClear" -> dungeonClear(c);
      case "dungeonReached" -> dungeonReached(c);
      case "gold" -> gold(c);
      case "enemyKillCount" -> enemyKillCount(c);
      case "killStreak" -> killStreak(c);
      case "speedRun" -> speedRun(c);
      case "enemySet" -> enemySet(c);
      default -> throw new IllegalArgumentException("Unknown achievement type: " + c.type);
    };
  }

  private static Achievement dungeonClear(AchievementConfig c) {
    return new Achievement(c.name, ctx -> c.dungeonId.equals(ctx.dungeonCompletedId));
  }

  private static Achievement gold(AchievementConfig c) {
    return new Achievement(c.name, ctx -> ctx.goldTotal != null && ctx.goldTotal >= c.target);
  }

  private static Achievement enemyKillCount(AchievementConfig c) {
    int[] count = {0};
    return new Achievement(
        c.name,
        ctx -> {
          if (ctx.enemyKilled == null) return false;
          if (c.enemyType != null && ctx.enemyKilled != c.enemyType) return false;
          return ++count[0] >= c.target;
        });
  }

  private static Achievement killStreak(AchievementConfig c) {
    int[] streak = {0};
    return new Achievement(
        c.name,
        ctx -> {
          if (ctx.playerDamaged) {
            streak[0] = 0;
            return false;
          }
          if (ctx.enemyKilled == null) return false;
          return ++streak[0] >= c.target;
        });
  }

  private static Achievement dungeonReached(AchievementConfig c) {
    return new Achievement(c.name, ctx -> c.dungeonId.equals(ctx.dungeonEnteredId));
  }

  private static Achievement speedRun(AchievementConfig c) {
    float[] lastKnownTime = {Float.MAX_VALUE};
    return new Achievement(
        c.name,
        ctx -> {
          if (c.dungeonId.equals(ctx.dungeonId) && ctx.dungeonSeconds > 0f) {
            lastKnownTime[0] = ctx.dungeonSeconds;
          }
          return c.dungeonId.equals(ctx.dungeonCompletedId) && lastKnownTime[0] <= c.maxSeconds;
        });
  }

  private static Achievement enemySet(AchievementConfig c) {
    Set<EnemySpawnConfig.EnemyType> required = EnumSet.copyOf(c.enemyTypes);
    Set<EnemySpawnConfig.EnemyType> seen = EnumSet.noneOf(EnemySpawnConfig.EnemyType.class);
    return new Achievement(
        c.name,
        ctx -> {
          if (ctx.enemyKilled == null || !required.contains(ctx.enemyKilled)) {
            return false;
          }
          seen.add(ctx.enemyKilled);
          return seen.containsAll(required);
        });
  }

  private AchievementsFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
