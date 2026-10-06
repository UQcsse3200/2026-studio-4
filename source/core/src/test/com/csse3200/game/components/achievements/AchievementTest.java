package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests the shared behaviour of the abstract {@link Achievement} base class. */
class AchievementTest {
  private static final String NAME = "Base Achievement";
  private Achievement achievement;

  @BeforeEach
  void setUp() {
    // Mockito creates the abstract class via its real constructor and runs real methods.
    achievement =
        mock(
            Achievement.class,
            withSettings().useConstructor(NAME).defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  void getNameReturnsConstructorName() {
    assertEquals(NAME, achievement.getName());
  }

  @Test
  void startsLocked() {
    assertFalse(achievement.isUnlocked());
  }

  @Test
  void unlockReturnsTrueOnlyOnFirstCall() {
    assertTrue(achievement.unlock());
    assertTrue(achievement.isUnlocked());
    assertFalse(achievement.unlock());
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void defaultHooksReturnFalseAndDoNotUnlock() {
    assertFalse(achievement.onEnemyDied(EnemyType.ZOMBIE));
    assertFalse(achievement.onPlayerDamaged());
    assertFalse(achievement.onDungeonCompleted("d1"));
    assertFalse(achievement.onDungeonEntered("d1"));
    assertFalse(achievement.onDungeonTimeElapsed("d1", 10f));
    assertFalse(achievement.onConsumableUsed());
    assertFalse(achievement.onGoldChanged(100));
    assertFalse(achievement.isUnlocked());
  }
}
