package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnemySetAchievementTest {
  private Set<EnemyType> required;
  private EnemySetAchievement achievement;

  @BeforeEach
  void setUp() {
    required = EnumSet.of(EnemyType.ZOMBIE, EnemyType.KNIGHT);
    achievement = new EnemySetAchievement(required, "Collector");
  }

  @Test
  void initialCounts() {
    assertEquals("Collector", achievement.getName());
    assertEquals(0, achievement.getSeenCount());
    assertEquals(2, achievement.getRequiredCount());
  }

  @Test
  void unlocksOnlyAfterAllRequiredTypesKilled() {
    assertFalse(achievement.onEnemyDied(EnemyType.ZOMBIE));
    assertEquals(1, achievement.getSeenCount());
    assertFalse(achievement.isUnlocked());

    assertTrue(achievement.onEnemyDied(EnemyType.KNIGHT));
    assertEquals(2, achievement.getSeenCount());
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void repeatKillsOfSameTypeDoNotCountTwice() {
    achievement.onEnemyDied(EnemyType.ZOMBIE);
    achievement.onEnemyDied(EnemyType.ZOMBIE);
    achievement.onEnemyDied(EnemyType.ZOMBIE);
    assertEquals(1, achievement.getSeenCount());
    assertFalse(achievement.isUnlocked());
  }

  @Test
  void typesOutsideTheRequiredSetAreIgnored() {
    assertFalse(achievement.onEnemyDied(EnemyType.SNAKE_MINI_BOSS));
    assertEquals(0, achievement.getSeenCount());
  }

  @Test
  void returnsFalseOnceAlreadyUnlocked() {
    achievement.onEnemyDied(EnemyType.ZOMBIE);
    achievement.onEnemyDied(EnemyType.KNIGHT);
    assertFalse(achievement.onEnemyDied(EnemyType.ZOMBIE));
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void killOrderDoesNotMatter() {
    assertFalse(achievement.onEnemyDied(EnemyType.KNIGHT));
    assertTrue(achievement.onEnemyDied(EnemyType.ZOMBIE));
  }

  @Test
  void constructorCopiesRequiredSet() {
    required.add(EnemyType.SNAKE_MINI_BOSS); // mutate caller's set after construction
    assertEquals(2, achievement.getRequiredCount());
    achievement.onEnemyDied(EnemyType.ZOMBIE);
    assertTrue(achievement.onEnemyDied(EnemyType.KNIGHT));
  }

  @Test
  void singleTypeSetUnlocksOnFirstKill() {
    EnemySetAchievement single =
        new EnemySetAchievement(EnumSet.of(EnemyType.SNAKE_MINI_BOSS), "Boss Slayer");
    assertTrue(single.onEnemyDied(EnemyType.SNAKE_MINI_BOSS));
  }
}
