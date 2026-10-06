// package com.csse3200.game.components.achievements;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertTrue;
//
// import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
//
// class KillStreakAchievementTest {
//  private KillStreakAchievement achievement;
//
//  @BeforeEach
//  void setUp() {
//    achievement = new KillStreakAchievement(3, "Untouchable");
//  }
//
//  @Test
//  void startsWithZeroStreak() {
//    assertEquals("Untouchable", achievement.getName());
//    assertEquals(0, achievement.getStreak());
//  }
//
//  @Test
//  void streakIncrementsPerKillRegardlessOfType() {
//    achievement.onEnemyDied(EnemyType.ZOMBIE);
//    achievement.onEnemyDied(EnemyType.SNAKE_MINI_BOSS);
//    assertEquals(2, achievement.getStreak());
//    assertFalse(achievement.isUnlocked());
//  }
//
//  @Test
//  void unlocksWhenStreakReachesTarget() {
//    assertFalse(achievement.onEnemyDied(EnemyType.ZOMBIE));
//    assertFalse(achievement.onEnemyDied(EnemyType.ZOMBIE));
//    assertTrue(achievement.onEnemyDied(EnemyType.ZOMBIE));
//    assertTrue(achievement.isUnlocked());
//  }
//
//  @Test
//  void damageResetsStreak() {
//    achievement.onEnemyDied(EnemyType.ZOMBIE);
//    achievement.onEnemyDied(EnemyType.ZOMBIE);
//    assertFalse(achievement.onPlayerDamaged());
//    assertEquals(0, achievement.getStreak());
//    assertFalse(achievement.onEnemyDied(EnemyType.ZOMBIE));
//    assertFalse(achievement.isUnlocked());
//  }
//
//  @Test
//  void damageNeverUnlocks() {
//    assertFalse(achievement.onPlayerDamaged());
//    assertFalse(achievement.isUnlocked());
//  }
//
//  @Test
//  void needsFullStreakAfterReset() {
//    achievement.onEnemyDied(EnemyType.ZOMBIE);
//    achievement.onEnemyDied(EnemyType.ZOMBIE);
//    achievement.onPlayerDamaged();
//    achievement.onEnemyDied(EnemyType.ZOMBIE);
//    achievement.onEnemyDied(EnemyType.ZOMBIE);
//    assertTrue(achievement.onEnemyDied(EnemyType.ZOMBIE));
//  }
//
//  @Test
//  void afterUnlockKillsAreIgnoredAndStreakStopsCounting() {
//    achievement.onEnemyDied(EnemyType.ZOMBIE);
//    achievement.onEnemyDied(EnemyType.ZOMBIE);
//    achievement.onEnemyDied(EnemyType.ZOMBIE);
//    assertFalse(achievement.onEnemyDied(EnemyType.ZOMBIE));
//    assertEquals(3, achievement.getStreak());
//  }
// }
