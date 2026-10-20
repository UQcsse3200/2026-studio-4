package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the achievement core: {@link Achievement} behaviour, the {@link AchievementsFactory} type
 * rules, and the shipped {@code configs/achievements.json}.
 */
@ExtendWith(GameExtension.class)
class AchievementsTest {
  private static final AchievementContext ANY_CONTEXT = new AchievementContext();
  private static final String DESC = "description";

  private static AchievementConfig config(String type) {
    AchievementConfig c = new AchievementConfig();
    c.type = type;
    c.name = type + " test";
    c.description = DESC;
    c.category = "Test";
    return c;
  }

  private static AchievementContext kill(EnemyType type) {
    AchievementContext context = new AchievementContext();
    context.enemyKilled = type;
    return context;
  }

  // ---------------------------------------------------------------------------------------------
  // Achievement
  // ---------------------------------------------------------------------------------------------

  @Nested
  class AchievementBehaviour {

    @Test
    void constructors_keepNameAndDescriptionAndStartLocked() {
      Achievement withCondition = new Achievement("A", "descA", ctx -> true);
      Achievement withoutCondition = new Achievement("B", "descB");

      assertEquals("A", withCondition.getName());
      assertEquals("descA", withCondition.getDescription());
      assertEquals("B", withoutCondition.getName());
      assertEquals("descB", withoutCondition.getDescription());
      assertFalse(withCondition.isUnlocked());
      assertFalse(withoutCondition.isUnlocked());
    }

    @Test
    void nullDescription_isNull() {
      assertNull(new Achievement("Slayer", null, ctx -> true).getDescription());
      assertNull(new Achievement("Slayer", null).getDescription());
    }

    @Test
    void update_conditionNotMet_returnsFalseAndStaysLocked() {
      Achievement achievement = new Achievement("Never", DESC, ctx -> false);

      assertFalse(achievement.update(ANY_CONTEXT));
      assertFalse(achievement.isUnlocked());
    }

    @Test
    void update_conditionMet_returnsTrueAndUnlocks() {
      Achievement achievement = new Achievement("Always", DESC, ctx -> true);

      assertTrue(achievement.update(ANY_CONTEXT));
      assertTrue(achievement.isUnlocked());
    }

    @Test
    void update_passesContextToCondition() {
      AchievementContext context = new AchievementContext();
      context.goldCollected = 50;
      Achievement achievement =
          new Achievement(
              "Rich", DESC, ctx -> ctx.goldCollected != null && ctx.goldCollected == 50);

      assertTrue(achievement.update(context));
    }

    @Test
    void update_afterUnlocking_reportsUnlockOnlyOnce() {
      Achievement achievement = new Achievement("Always", DESC, ctx -> true);

      assertTrue(achievement.update(ANY_CONTEXT));
      assertFalse(achievement.update(ANY_CONTEXT));
    }

    @Test
    void update_afterUnlocking_staysUnlockedEvenIfConditionLaterFails() {
      AchievementContext rich = new AchievementContext();
      rich.goldCollected = 100;
      AchievementContext poor = new AchievementContext();
      poor.goldCollected = 0;
      Achievement achievement =
          new Achievement(
              "Rich", DESC, ctx -> ctx.goldCollected != null && ctx.goldCollected >= 100);

      achievement.update(rich);
      achievement.update(poor);

      assertTrue(achievement.isUnlocked());
    }

    @Test
    void update_afterUnlocking_doesNotEvaluateConditionAgain() {
      AtomicInteger evaluations = new AtomicInteger();
      Achievement achievement =
          new Achievement(
              "Counted",
              DESC,
              ctx -> {
                evaluations.incrementAndGet();
                return true;
              });

      achievement.update(ANY_CONTEXT);
      achievement.update(ANY_CONTEXT);
      achievement.update(ANY_CONTEXT);

      assertEquals(1, evaluations.get());
    }

    @Test
    void update_conditionFailsThenPasses_unlocksOnLaterUpdate() {
      AchievementContext now = new AchievementContext();
      now.dungeonCompletedId = "forest";
      Achievement achievement =
          new Achievement("Forest", DESC, ctx -> "forest".equals(ctx.dungeonCompletedId));

      assertFalse(achievement.update(new AchievementContext()));
      assertTrue(achievement.update(now));
    }

    @Test
    void setCondition_attachedAfterConstruction_isUsedByUpdate() {
      Achievement achievement = new Achievement("Late", DESC);
      achievement.setCondition(ctx -> true);

      assertTrue(achievement.update(ANY_CONTEXT));
    }

    @Test
    void progressAndTarget_startAtZeroAndAccumulate() {
      Achievement achievement = new Achievement("Counter", DESC);
      assertEquals(0f, achievement.getProgress());
      assertEquals(0f, achievement.getTarget());

      achievement.setTarget(25f);
      achievement.addProgress(2f);
      achievement.addProgress(3f);
      assertEquals(25f, achievement.getTarget());
      assertEquals(5f, achievement.getProgress());

      achievement.setProgress(1f);
      assertEquals(1f, achievement.getProgress());
    }

    @Test
    void restoreState_setsUnlockedAndProgress() {
      Achievement achievement = new Achievement("Counter", DESC, ctx -> true);
      achievement.restoreState(true, 7f);

      assertTrue(achievement.isUnlocked());
      assertEquals(7f, achievement.getProgress());
      assertFalse(achievement.update(ANY_CONTEXT)); // restored unlock is not re-announced
    }
  }

  // ---------------------------------------------------------------------------------------------
  // AchievementsFactory
  // ---------------------------------------------------------------------------------------------

  @Nested
  class FactoryRules {

    @Test
    void unknownTypeThrows() {
      assertThrows(IllegalArgumentException.class, () -> AchievementsFactory.build(config("nope")));
    }

    @Test
    void build_copiesNameDescriptionAndCategory() {
      Achievement a = AchievementsFactory.build(config("gold"));

      assertEquals("gold test", a.getName());
      assertEquals(DESC, a.getDescription());
      assertEquals("Test", a.getCategory());
    }

    @Test
    void dungeonClear_matchesOnlyItsDungeon() {
      AchievementConfig c = config("dungeonClear");
      c.dungeonId = "dungeonOne";
      Achievement a = AchievementsFactory.build(c);

      AchievementContext other = new AchievementContext();
      other.dungeonCompletedId = "dungeonTwo";
      assertFalse(a.update(other));

      AchievementContext match = new AchievementContext();
      match.dungeonCompletedId = "dungeonOne";
      assertTrue(a.update(match));
    }

    @Test
    void dungeonReached_matchesEnteredId() {
      AchievementConfig c = config("dungeonReached");
      c.dungeonId = "finalDungeon";
      Achievement a = AchievementsFactory.build(c);

      assertFalse(a.update(new AchievementContext()));

      AchievementContext ctx = new AchievementContext();
      ctx.dungeonEnteredId = "finalDungeon";
      assertTrue(a.update(ctx));
    }

    @Test
    void gold_countsPickupsUntilTarget() {
      AchievementConfig c = config("gold");
      c.target = 3;
      Achievement a = AchievementsFactory.build(c);

      assertFalse(a.update(new AchievementContext())); // goldCollected == null
      assertEquals(0f, a.getProgress());

      AchievementContext pickup = new AchievementContext();
      pickup.goldCollected = 5;
      assertFalse(a.update(pickup));
      assertFalse(a.update(pickup));
      assertTrue(a.update(pickup));
      assertEquals(3f, a.getProgress());
    }

    @Test
    void enemyKillCount_withNoTypeCountsAnyEnemy() {
      AchievementConfig c = config("enemyKillCount");
      c.target = 2;
      Achievement a = AchievementsFactory.build(c);

      assertFalse(a.update(kill(EnemyType.WOLF)));
      assertTrue(a.update(kill(EnemyType.MEDUSA)));
    }

    @Test
    void killStreak_resetsWhenPlayerDamaged() {
      AchievementConfig c = config("killStreak");
      c.target = 3;
      Achievement a = AchievementsFactory.build(c);

      assertFalse(a.update(kill(EnemyType.WOLF)));
      assertFalse(a.update(kill(EnemyType.WOLF)));

      AchievementContext hit = new AchievementContext();
      hit.playerDamaged = true;
      assertFalse(a.update(hit));
      assertEquals(0f, a.getProgress());

      assertFalse(a.update(kill(EnemyType.WOLF)));
      assertFalse(a.update(kill(EnemyType.WOLF)));
      assertTrue(a.update(kill(EnemyType.WOLF)));
    }

    @Test
    void speedRun_unlocksOnlyWhenCompletedUnderLimit() {
      AchievementConfig c = config("speedRun");
      c.dungeonId = "dungeonOne";
      c.maxSeconds = 60;
      Achievement a = AchievementsFactory.build(c);

      AchievementContext slow = new AchievementContext();
      slow.dungeonId = "dungeonOne";
      slow.dungeonCompletedId = "dungeonOne";
      slow.dungeonSeconds = 75f;
      assertFalse(a.update(slow));

      AchievementContext fast = new AchievementContext();
      fast.dungeonId = "dungeonOne";
      fast.dungeonCompletedId = "dungeonOne";
      fast.dungeonSeconds = 45f;
      assertTrue(a.update(fast));
    }

    @Test
    void speedRun_ignoresFastTimeWithoutCompletion() {
      AchievementConfig c = config("speedRun");
      c.dungeonId = "dungeonOne";
      c.maxSeconds = 60;
      Achievement a = AchievementsFactory.build(c);

      AchievementContext ctx = new AchievementContext();
      ctx.dungeonId = "dungeonOne";
      ctx.dungeonSeconds = 10f; // no dungeonCompletedId
      assertFalse(a.update(ctx));
    }

    @Test
    void enemySet_needsEveryRequiredType() {
      AchievementConfig c = config("enemySet");
      c.enemyTypes = List.of(EnemyType.MEDUSA, EnemyType.WOLF);
      Achievement a = AchievementsFactory.build(c);

      assertFalse(a.update(kill(EnemyType.MEDUSA)));
      assertFalse(a.update(kill(EnemyType.MEDUSA))); // repeat doesn't count twice
      assertFalse(a.update(kill(EnemyType.CROW))); // not in the set
      assertTrue(a.update(kill(EnemyType.WOLF)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"snake", "finalBoss"})
    void bossTypes_matchOnBossAchievementId(String type) {
      AchievementConfig c = config(type);
      c.bossAchievementId = "someId";
      Achievement a = AchievementsFactory.build(c);

      AchievementContext other = new AchievementContext();
      other.bossAchievementId = "otherId";
      assertFalse(a.update(other));
      assertFalse(a.update(new AchievementContext())); // null id

      AchievementContext match = new AchievementContext();
      match.bossAchievementId = "someId";
      assertTrue(a.update(match));
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Shipped configs/achievements.json
  // ---------------------------------------------------------------------------------------------

  @Nested
  class ShippedConfig {
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
  }
}
