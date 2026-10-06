// package com.csse3200.game.components.achievements;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertTrue;
//
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
//
// class DungeonReachedAchievementTest {
//  private DungeonReachedAchievement achievement;
//
//  @BeforeEach
//  void setUp() {
//    achievement = new DungeonReachedAchievement("cave", "Cave Explorer");
//  }
//
//  @Test
//  void hasGivenName() {
//    assertEquals("Cave Explorer", achievement.getName());
//  }
//
//  @Test
//  void unlocksWhenMatchingDungeonEntered() {
//    assertTrue(achievement.onDungeonEntered("cave"));
//    assertTrue(achievement.isUnlocked());
//  }
//
//  @Test
//  void doesNotUnlockForDifferentDungeon() {
//    assertFalse(achievement.onDungeonEntered("forest"));
//    assertFalse(achievement.isUnlocked());
//  }
//
//  @Test
//  void returnsTrueOnlyTheFirstTime() {
//    assertTrue(achievement.onDungeonEntered("cave"));
//    assertFalse(achievement.onDungeonEntered("cave"));
//  }
//
//  @Test
//  void ignoresOtherEvents() {
//    assertFalse(achievement.onDungeonCompleted("cave"));
//    assertFalse(achievement.isUnlocked());
//  }
// }
