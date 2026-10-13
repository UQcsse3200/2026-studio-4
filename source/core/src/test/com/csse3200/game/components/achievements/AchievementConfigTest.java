package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Tests the shipped achievement configuration and its expected unlock rules. */
@ExtendWith(GameExtension.class)
class AchievementConfigTest {
  private AchievementConfig[] configs;

  @BeforeEach
  void setUp() {
    configs = FileLoader.readClass(AchievementConfig[].class, "configs/achievements.json");
    assertNotNull(configs);
    assertTrue(configs.length > 0, "Achievement configuration must not be empty");
  }

  @Test
  void shouldBuildEveryConfiguredAchievementWithAUniqueName() {
    Set<String> names = new HashSet<>();

    for (AchievementConfig config : configs) {
      assertNotNull(config);
      assertNotNull(config.name);
      assertFalse(config.name.isBlank(), "Achievement name must not be blank");
      assertTrue(names.add(config.name), "Duplicate achievement name: " + config.name);
      assertNotNull(
          AchievementsFactory.build(config), "Could not build achievement: " + config.name);
    }
  }

  @Test
  void shouldUnlockConfiguredMinibossAchievementsOnlyForMatchingKills() {
    verifyAchievement("Dragon Slayer", EnemyType.DRAGON);
    verifyAchievement("Serpent Slayer", EnemyType.SNAKE_MINI_BOSS);
    verifyAchievement("Eight Legs, No Escape", EnemyType.SLEIPNIR);
    verifyAchievement("Three Heads, One Victory", EnemyType.CERBERUS);
  }

  private void verifyAchievement(String name, EnemyType expectedType) {
    List<AchievementConfig> matches =
        Arrays.stream(configs).filter(config -> name.equals(config.name)).toList();

    assertEquals(1, matches.size(), "Expected exactly one entry for " + name);

    AchievementConfig config = matches.get(0);
    assertEquals("enemyKillCount", config.type, name);
    assertEquals(expectedType, config.enemyType, name);
    assertEquals(1f, config.target, 0.001f, name);

    Achievement achievement = AchievementsFactory.build(config);

    assertFalse(achievement.update(new AchievementContext()), name);
    assertFalse(achievement.update(kill(EnemyType.MEDUSA)), name);
    assertFalse(achievement.isUnlocked(), name);

    assertTrue(achievement.update(kill(expectedType)), name);
    assertTrue(achievement.isUnlocked(), name);

    // A repeated kill must not announce the achievement again.
    assertFalse(achievement.update(kill(expectedType)), name);
  }

  private AchievementContext kill(EnemyType type) {
    AchievementContext context = new AchievementContext();
    context.enemyKilled = type;
    return context;
  }
}
