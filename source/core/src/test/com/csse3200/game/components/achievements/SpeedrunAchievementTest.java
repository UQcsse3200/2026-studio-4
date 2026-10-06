// package com.csse3200.game.components.achievements;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertTrue;
//
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
//
// class SpeedRunAchievementTest {
//  private SpeedRunAchievement achievement;
//
//  @BeforeEach
//  void setUp() {
//    achievement = new SpeedRunAchievement("forest", 60f, "Speedy");
//  }
//
//  @Test
//  void hasGivenName() {
//    assertEquals("Speedy", achievement.getName());
//  }
//
//  @Test
//  void timeUpdateNeverUnlocksByItself() {
//    assertFalse(achievement.onDungeonTimeElapsed("forest", 10f));
//    assertFalse(achievement.isUnlocked());
//  }
//
//  @Test
//  void unlocksWhenCompletedUnderTimeLimit() {
//    achievement.onDungeonTimeElapsed("forest", 45f);
//    assertTrue(achievement.onDungeonCompleted("forest"));
//    assertTrue(achievement.isUnlocked());
//  }
//
//  @Test
//  void unlocksWhenCompletedExactlyAtLimit() {
//    achievement.onDungeonTimeElapsed("forest", 60f);
//    assertTrue(achievement.onDungeonCompleted("forest"));
//  }
//
//  @Test
//  void doesNotUnlockWhenOverTimeLimit() {
//    achievement.onDungeonTimeElapsed("forest", 60.01f);
//    assertFalse(achievement.onDungeonCompleted("forest"));
//    assertFalse(achievement.isUnlocked());
//  }
//
//  @Test
//  void usesMostRecentTimeReported() {
//    achievement.onDungeonTimeElapsed("forest", 30f);
//    achievement.onDungeonTimeElapsed("forest", 90f);
//    assertFalse(achievement.onDungeonCompleted("forest"));
//  }
//
//  @Test
//  void timeForOtherDungeonIsIgnored() {
//    achievement.onDungeonTimeElapsed("cave", 5f);
//    assertFalse(achievement.onDungeonCompleted("forest"));
//  }
//
//  @Test
//  void completingOtherDungeonDoesNotUnlock() {
//    achievement.onDungeonTimeElapsed("forest", 10f);
//    assertFalse(achievement.onDungeonCompleted("cave"));
//    assertFalse(achievement.isUnlocked());
//  }
//
//  @Test
//  void returnsTrueOnlyTheFirstTime() {
//    achievement.onDungeonTimeElapsed("forest", 10f);
//    assertTrue(achievement.onDungeonCompleted("forest"));
//    assertFalse(achievement.onDungeonCompleted("forest"));
//  }
// }
