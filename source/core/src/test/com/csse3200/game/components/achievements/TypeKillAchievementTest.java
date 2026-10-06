package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import org.junit.jupiter.api.Test;

class TypeKillAchievementTest {

  @Test
  void exposesTargetCountAndName() {
    TypeKillAchievement a = new TypeKillAchievement(EnemyType.ZOMBIE, 3, "Zombie Hunter");
    assertEquals("Zombie Hunter", a.getName());
    assertEquals(3, a.getTarget());
    assertEquals(0, a.getCount());
  }

  @Test
  void countsOnlyMatchingType() {
    TypeKillAchievement a = new TypeKillAchievement(EnemyType.ZOMBIE, 3, "Zombie Hunter");
    assertFalse(a.onEnemyDied(EnemyType.KNIGHT));
    assertEquals(0, a.getCount());
    assertFalse(a.onEnemyDied(EnemyType.ZOMBIE));
    assertEquals(1, a.getCount());
  }

  @Test
  void unlocksOnReachingTarget() {
    TypeKillAchievement a = new TypeKillAchievement(EnemyType.ZOMBIE, 2, "Zombie Hunter");
    assertFalse(a.onEnemyDied(EnemyType.ZOMBIE));
    assertTrue(a.onEnemyDied(EnemyType.ZOMBIE));
    assertTrue(a.isUnlocked());
  }

  @Test
  void mismatchedKillsBetweenMatchesDoNotBreakProgress() {
    TypeKillAchievement a = new TypeKillAchievement(EnemyType.ZOMBIE, 2, "Zombie Hunter");
    a.onEnemyDied(EnemyType.ZOMBIE);
    a.onEnemyDied(EnemyType.SNAKE_MINI_BOSS);
    assertTrue(a.onEnemyDied(EnemyType.ZOMBIE));
  }

  @Test
  void nullTypeCountsAnyEnemy() {
    TypeKillAchievement a = new TypeKillAchievement(null, 3, "Slayer");
    a.onEnemyDied(EnemyType.ZOMBIE);
    a.onEnemyDied(EnemyType.SNAKE_MINI_BOSS);
    assertTrue(a.onEnemyDied(EnemyType.SNAKE_MINI_BOSS));
    assertEquals(3, a.getCount());
  }

  @Test
  void stopsCountingAfterUnlock() {
    TypeKillAchievement a = new TypeKillAchievement(EnemyType.ZOMBIE, 1, "First Blood");
    assertTrue(a.onEnemyDied(EnemyType.ZOMBIE));
    assertFalse(a.onEnemyDied(EnemyType.ZOMBIE));
    assertEquals(1, a.getCount());
  }

  @Test
  void ignoresOtherEvents() {
    TypeKillAchievement a = new TypeKillAchievement(EnemyType.ZOMBIE, 1, "First Blood");
    assertFalse(a.onPlayerDamaged());
    assertFalse(a.onGoldChanged(1000));
    assertFalse(a.isUnlocked());
  }
}
