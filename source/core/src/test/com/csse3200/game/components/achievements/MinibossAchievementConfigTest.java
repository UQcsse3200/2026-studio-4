package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MinibossAchievementConfigTest {
  @Test
  void shouldUnlockConfiguredMinibossAchievementsOnlyForMatchingKills() {
    AchievementConfig[] configs =
        FileLoader.readClass(AchievementConfig[].class, "configs/achievements.json");

    assertNotNull(configs);

    verifyAchievement(configs, "Dragon Slayer", EnemyType.DRAGON);
    verifyAchievement(configs, "Serpent Slayer", EnemyType.SNAKE_MINI_BOSS);
    verifyAchievement(configs, "Eight Legs, No Escape", EnemyType.SLEIPNIR);
    verifyAchievement(configs, "Three Heads, One Victory", EnemyType.CERBERUS);
  }

  private void verifyAchievement(AchievementConfig[] configs, String name, EnemyType expectedType) {
    List<AchievementConfig> matches =
        Arrays.stream(configs).filter(config -> name.equals(config.name)).toList();

    assertEquals(1, matches.size(), "Expected exactly one entry for " + name);

    AchievementConfig config = matches.get(0);
    assertEquals("enemyKillCount", config.type);
    assertEquals(expectedType, config.enemyType);
    assertEquals(1f, config.target, 0.001f);

    Achievement achievement = AchievementsFactory.build(config);

    assertFalse(achievement.update(new AchievementContext()));
    assertFalse(achievement.update(kill(EnemyType.MEDUSA)));
    assertFalse(achievement.isUnlocked());

    assertTrue(achievement.update(kill(expectedType)));
    assertTrue(achievement.isUnlocked());

    // A repeated kill must not announce the achievement again.
    assertFalse(achievement.update(kill(expectedType)));
  }

  private AchievementContext kill(EnemyType type) {
    AchievementContext context = new AchievementContext();
    context.enemyKilled = type;
    return context;
  }
}
