package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DungeonClearAchievementTest {
  private DungeonClearAchievement achievement;

  @BeforeEach
  void setUp() {
    achievement = new DungeonClearAchievement("forest", "Forest Cleared");
  }

  @Test
  void hasGivenName() {
    assertEquals("Forest Cleared", achievement.getName());
  }

  @Test
  void unlocksWhenMatchingDungeonCompleted() {
    assertTrue(achievement.onDungeonCompleted("forest"));
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void doesNotUnlockForDifferentDungeon() {
    assertFalse(achievement.onDungeonCompleted("cave"));
    assertFalse(achievement.isUnlocked());
  }

  @Test
  void returnsTrueOnlyTheFirstTime() {
    assertTrue(achievement.onDungeonCompleted("forest"));
    assertFalse(achievement.onDungeonCompleted("forest"));
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void ignoresOtherEvents() {
    assertFalse(achievement.onDungeonEntered("forest"));
    assertFalse(achievement.onDungeonTimeElapsed("forest", 1f));
    assertFalse(achievement.isUnlocked());
  }
}
