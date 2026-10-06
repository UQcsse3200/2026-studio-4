package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoldAchievementTest {
  private GoldAchievement achievement;

  @BeforeEach
  void setUp() {
    achievement = new GoldAchievement(100, "Rich");
  }

  @Test
  void hasGivenName() {
    assertEquals("Rich", achievement.getName());
  }

  @Test
  void doesNotUnlockBelowTarget() {
    assertFalse(achievement.onGoldChanged(99));
    assertFalse(achievement.isUnlocked());
  }

  @Test
  void unlocksExactlyAtTarget() {
    assertTrue(achievement.onGoldChanged(100));
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void unlocksAboveTarget() {
    assertTrue(achievement.onGoldChanged(500));
  }

  @Test
  void returnsTrueOnlyTheFirstTime() {
    assertTrue(achievement.onGoldChanged(100));
    assertFalse(achievement.onGoldChanged(200));
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void unlocksAfterProgressingThroughMultipleUpdates() {
    assertFalse(achievement.onGoldChanged(40));
    assertFalse(achievement.onGoldChanged(80));
    assertTrue(achievement.onGoldChanged(120));
  }

  @Test
  void ignoresOtherEvents() {
    assertFalse(achievement.onPlayerDamaged());
    assertFalse(achievement.isUnlocked());
  }
}
