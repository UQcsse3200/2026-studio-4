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
      case "finalBossBreakRespected" -> finalBossBreakRespected(c);
      default -> throw new IllegalArgumentException("Unknown achievement type: " + c.type);
    };
  }

  private static Achievement dungeonClear(AchievementConfig c) {
    return new Achievement(c.name, ctx -> c.dungeonId.equals(ctx.dungeonCompletedId));
  }

  private static Achievement finalBossBreakRespected(AchievementConfig c) {
    return new Achievement(c.name, ctx -> ctx.finalBossBreakRespected);
  }

  private static Achievement gold(AchievementConfig c) {
    Achievement a = new Achievement(c.name);
    a.setTarget(c.target);
    boolean[] initial = {true};
    float[] initialGold = {0f};
    a.setCondition(
        ctx -> {
          if (ctx.goldTotal == null) {
            return false;
          }
          if (initial[0]) {
            initialGold[0] = ctx.goldTotal;
            initial[0] = false;
          }
          a.setProgress(ctx.goldTotal - initialGold[0]);
          return a.getProgress() >= c.target;
        });
    return a;
  }

  private static Achievement enemyKillCount(AchievementConfig c) {
    Achievement a = new Achievement(c.name);
    a.setTarget(c.target);
    a.setCondition(
        ctx -> {
          if (ctx.enemyKilled == null) return false;
          if (c.enemyType != null && ctx.enemyKilled != c.enemyType) return false;
          a.addProgress(1);
          return a.getProgress() >= c.target;
        });
    return a;
  }

  private static Achievement killStreak(AchievementConfig c) {
    Achievement a = new Achievement(c.name);
    a.setTarget(c.target);
    a.setCondition(
        ctx -> {
          if (ctx.playerDamaged) {
            a.setProgress(0);
            return false;
          }
          if (ctx.enemyKilled == null) return false;
          a.addProgress(1);
          return a.getProgress() >= c.target;
        });
    return a;
  }

  private static Achievement dungeonReached(AchievementConfig c) {
    return new Achievement(c.name, ctx -> c.dungeonId.equals(ctx.dungeonEnteredId));
  }

  private static Achievement speedRun(AchievementConfig c) {
    Achievement a = new Achievement(c.name);
    a.setProgress(
        Float.MAX_VALUE); // no time recorded yet; never beats the limit until a real reading lands
    a.setCondition(
        ctx -> {
          if (c.dungeonId.equals(ctx.dungeonId) && ctx.dungeonSeconds > 0f) {
            a.setProgress(ctx.dungeonSeconds);
          }
          return c.dungeonId.equals(ctx.dungeonCompletedId) && a.getProgress() <= c.maxSeconds;
        });
    return a;
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
