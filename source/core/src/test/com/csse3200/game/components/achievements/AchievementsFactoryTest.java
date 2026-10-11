package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AchievementsFactoryTest {

  // ---------- helpers ----------

  private static AchievementConfig config(String type) {
    AchievementConfig c = new AchievementConfig();
    c.type = type;
    c.name = "test-" + type;
    return c;
  }

  private static AchievementConfig finalBossConfig(String bossAchievementId) {
    AchievementConfig c = config("finalBoss");
    c.name = "test-" + bossAchievementId;
    c.bossAchievementId = bossAchievementId;
    return c;
  }

  private static AchievementContext kill(EnemyType type) {
    AchievementContext ctx = new AchievementContext();
    ctx.enemyKilled = type;
    return ctx;
  }

  private static AchievementContext damaged() {
    AchievementContext ctx = new AchievementContext();
    ctx.playerDamaged = true;
    return ctx;
  }

  private static AchievementContext completed(String dungeonId) {
    AchievementContext ctx = new AchievementContext();
    ctx.dungeonCompletedId = dungeonId;
    return ctx;
  }

  private static AchievementContext entered(String dungeonId) {
    AchievementContext ctx = new AchievementContext();
    ctx.dungeonEnteredId = dungeonId;
    return ctx;
  }

  private static AchievementContext gold(Integer total) {
    AchievementContext ctx = new AchievementContext();
    ctx.goldTotal = total;
    return ctx;
  }

  private static AchievementContext timing(String dungeonId, float seconds) {
    AchievementContext ctx = new AchievementContext();
    ctx.dungeonId = dungeonId;
    ctx.dungeonSeconds = seconds;
    return ctx;
  }

  // ---------- build() / general ----------

  @Nested
  class Build {

    @Test
    void build_unknownType_throwsIllegalArgumentException() {
      AchievementConfig c = config("notARealType");

      assertThrows(IllegalArgumentException.class, () -> AchievementsFactory.build(c));
    }

    @Test
    void build_unknownType_messageContainsTheOffendingType() {
      AchievementConfig c = config("notARealType");

      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> AchievementsFactory.build(c));

      assertEquals("Unknown achievement type: notARealType", e.getMessage());
    }

    @Test
    void build_validType_returnsLockedAchievementWithConfiguredName() {
      AchievementConfig c = config("gold");
      c.name = "Gold Digger";
      c.target = 10;

      Achievement a = AchievementsFactory.build(c);

      assertEquals("Gold Digger", a.getName());
      assertFalse(a.isUnlocked());
    }

    @Test
    void build_everyKnownType_returnsAnAchievement() {
      AchievementConfig dungeon = config("dungeonClear");
      dungeon.dungeonId = "d";
      AchievementConfig reached = config("dungeonReached");
      reached.dungeonId = "d";
      AchievementConfig speed = config("speedRun");
      speed.dungeonId = "d";
      AchievementConfig set = config("enemySet");
      set.enemyTypes = List.of(EnemyType.KNIGHT);

      assertInstanceOf(Achievement.class, AchievementsFactory.build(dungeon));
      assertInstanceOf(Achievement.class, AchievementsFactory.build(reached));
      assertInstanceOf(Achievement.class, AchievementsFactory.build(config("gold")));
      assertInstanceOf(Achievement.class, AchievementsFactory.build(config("enemyKillCount")));
      assertInstanceOf(Achievement.class, AchievementsFactory.build(config("killStreak")));
      assertInstanceOf(Achievement.class, AchievementsFactory.build(speed));
      assertInstanceOf(Achievement.class, AchievementsFactory.build(set));
      assertInstanceOf(
          Achievement.class, AchievementsFactory.build(finalBossConfig("finalBossBreakRespected")));
      assertInstanceOf(
          Achievement.class, AchievementsFactory.build(finalBossConfig("finalBossStageTwoIceHit")));
      assertInstanceOf(
          Achievement.class,
          AchievementsFactory.build(finalBossConfig("finalBossStageTwoFireproof")));
      assertInstanceOf(
          Achievement.class,
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHit")));
      assertInstanceOf(
          Achievement.class,
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHitStreak")));
      assertInstanceOf(
          Achievement.class,
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeMeleeHit")));
      assertInstanceOf(
          Achievement.class,
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeStatueFlash")));
    }

    @Test
    void build_everyKnownType_startsWithZeroProgress() {
      AchievementConfig dungeon = config("dungeonClear");
      dungeon.dungeonId = "d";
      AchievementConfig reached = config("dungeonReached");
      reached.dungeonId = "d";
      AchievementConfig set = config("enemySet");
      set.enemyTypes = List.of(EnemyType.KNIGHT);

      assertEquals(0f, AchievementsFactory.build(dungeon).getProgress());
      assertEquals(0f, AchievementsFactory.build(reached).getProgress());
      assertEquals(0f, AchievementsFactory.build(config("gold")).getProgress());
      assertEquals(0f, AchievementsFactory.build(config("enemyKillCount")).getProgress());
      assertEquals(0f, AchievementsFactory.build(config("killStreak")).getProgress());
      assertEquals(0f, AchievementsFactory.build(set).getProgress());
      assertEquals(
          0f, AchievementsFactory.build(finalBossConfig("finalBossBreakRespected")).getProgress());
      assertEquals(
          0f, AchievementsFactory.build(finalBossConfig("finalBossStageTwoIceHit")).getProgress());
      assertEquals(
          0f,
          AchievementsFactory.build(finalBossConfig("finalBossStageTwoFireproof")).getProgress());
      assertEquals(
          0f,
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHit")).getProgress());
      assertEquals(
          0f,
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHitStreak"))
              .getProgress());
      assertEquals(
          0f,
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeMeleeHit")).getProgress());
      assertEquals(
          0f,
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeStatueFlash"))
              .getProgress());
    }

    @Test
    void build_speedRun_startsWithNoTimeRecordedSentinel() {
      AchievementConfig speed = config("speedRun");
      speed.dungeonId = "d";

      assertEquals(Float.MAX_VALUE, AchievementsFactory.build(speed).getProgress());
    }

    @Test
    void constructor_isPrivateAndThrowsIllegalStateException() throws Exception {
      Constructor<AchievementsFactory> constructor =
          AchievementsFactory.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      InvocationTargetException e =
          assertThrows(InvocationTargetException.class, constructor::newInstance);

      assertInstanceOf(IllegalStateException.class, e.getCause());
    }
  }

  // ---------- finalBoss ID matching ----------

  @Nested
  class FinalBoss {

    private static final Set<String> BOSS_ACHIEVEMENT_IDS =
        Set.of(
            "finalBossBreakRespected",
            "finalBossStageTwoIceHit",
            "finalBossStageTwoFireproof",
            "finalBossStageThreeIceHit",
            "finalBossStageThreeIceHitStreak",
            "finalBossStageThreeMeleeHit",
            "finalBossStageThreeStatueFlash");

    @Test
    void config_eachBossEventUnlocksOnlyItsMatchingAchievement() {
      AchievementConfig[] configs =
          new Json()
              .fromJson(AchievementConfig[].class, new FileHandle("configs/achievements.json"));
      List<AchievementConfig> bossConfigs =
          Arrays.stream(configs).filter(c -> "finalBoss".equals(c.type)).toList();
      assertEquals(BOSS_ACHIEVEMENT_IDS.size(), bossConfigs.size());
      assertEquals(
          BOSS_ACHIEVEMENT_IDS,
          bossConfigs.stream().map(c -> c.bossAchievementId).collect(Collectors.toSet()));

      for (String eventId : BOSS_ACHIEVEMENT_IDS) {
        AchievementContext ctx = new AchievementContext();
        ctx.bossAchievementId = eventId;
        for (AchievementConfig c : bossConfigs) {
          Achievement achievement = AchievementsFactory.build(c);
          boolean matching = eventId.equals(c.bossAchievementId);
          String description = eventId + " -> " + c.bossAchievementId;

          assertEquals(matching, achievement.update(ctx), description);
          assertEquals(matching, achievement.isUnlocked(), description);
          assertFalse(achievement.update(ctx), description);
          assertEquals(matching, achievement.isUnlocked(), description);
        }
      }
    }

    @Test
    void update_missingEmptyOrUnknownBossId_staysLocked() {
      for (String bossId : BOSS_ACHIEVEMENT_IDS) {
        Achievement achievement = AchievementsFactory.build(finalBossConfig(bossId));
        AchievementContext ctx = new AchievementContext();
        assertFalse(achievement.update(ctx), bossId);
        for (String unrelatedId : List.of("", "unknownFinalBossAchievement", bossId + "Typo")) {
          ctx.bossAchievementId = unrelatedId;
          assertFalse(achievement.update(ctx), bossId);
          assertFalse(achievement.isUnlocked(), bossId);
        }
      }
    }
  }

  // ---------- finalBossBreakRespected ----------

  @Nested
  class FinalBossBreakRespected {

    @Test
    void update_withoutCompletedBreak_staysLocked() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossBreakRespected"));

      assertFalse(a.update(new AchievementContext()));
      assertFalse(a.update(kill(EnemyType.FINAL_BOSS)));
      assertFalse(a.update(entered("finalDungeon")));
      assertFalse(a.update(completed("finalDungeon")));
      assertFalse(a.update(damaged()));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_completedBreak_unlocksOnlyOnce() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossBreakRespected"));
      AchievementContext ctx = new AchievementContext();
      ctx.bossAchievementId = "finalBossBreakRespected";

      assertTrue(a.update(ctx));
      assertTrue(a.isUnlocked());
      assertFalse(a.update(ctx));
      assertFalse(a.update(new AchievementContext()));
      assertTrue(a.isUnlocked());
    }

    @Test
    void config_registersOneBreakAchievementWithExpectedNameAndCondition() {
      AchievementConfig[] configs =
          new Json()
              .fromJson(AchievementConfig[].class, new FileHandle("configs/achievements.json"));
      int breakAchievements = 0;
      for (AchievementConfig c : configs) {
        Achievement a = AchievementsFactory.build(c);
        if ("finalBoss".equals(c.type) && "finalBossBreakRespected".equals(c.bossAchievementId)) {
          breakAchievements++;
          assertEquals("I need a break too", a.getName());
          assertFalse(a.update(new AchievementContext()));
          AchievementContext ctx = new AchievementContext();
          ctx.bossAchievementId = "finalBossBreakRespected";
          assertTrue(a.update(ctx));
        }
      }
      assertEquals(1, breakAchievements);
    }
  }

  // ---------- finalBossStageTwoIceHit ----------

  @Nested
  class FinalBossStageTwoIceHit {

    @Test
    void update_unrelatedDamageAndBossEvents_staysLocked() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossStageTwoIceHit"));
      AchievementContext breakCompleted = new AchievementContext();
      breakCompleted.bossAchievementId = "finalBossBreakRespected";
      AchievementContext snowQueen = new AchievementContext();
      snowQueen.bossAchievementId = "finalBossStageThreeIceHit";

      assertFalse(a.update(new AchievementContext()));
      assertFalse(a.update(damaged()));
      assertFalse(a.update(kill(EnemyType.FINAL_BOSS)));
      assertFalse(a.update(breakCompleted));
      assertFalse(a.update(snowQueen));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_firstPlayerIceHit_unlocksOnlyCoolinOffAndOnlyOnce() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossStageTwoIceHit"));
      Achievement breakAchievement =
          AchievementsFactory.build(finalBossConfig("finalBossBreakRespected"));
      Achievement snowQueen =
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHit"));
      AchievementContext ctx = new AchievementContext();
      ctx.bossAchievementId = "finalBossStageTwoIceHit";

      assertTrue(a.update(ctx));
      assertTrue(a.isUnlocked());
      assertFalse(a.update(ctx));
      assertFalse(breakAchievement.update(ctx));
      assertFalse(snowQueen.update(ctx));
    }

    @Test
    void config_registersOneCoolinOffAchievementWithExpectedNameAndCondition() {
      AchievementConfig[] configs =
          new Json()
              .fromJson(AchievementConfig[].class, new FileHandle("configs/achievements.json"));
      int iceHitAchievements = 0;
      for (AchievementConfig c : configs) {
        Achievement a = AchievementsFactory.build(c);
        if ("finalBoss".equals(c.type) && "finalBossStageTwoIceHit".equals(c.bossAchievementId)) {
          iceHitAchievements++;
          assertEquals("Coolin' off, make it fun", a.getName());
          assertFalse(a.update(new AchievementContext()));
          AchievementContext ctx = new AchievementContext();
          ctx.bossAchievementId = "finalBossStageTwoIceHit";
          assertTrue(a.update(ctx));
        }
      }
      assertEquals(1, iceHitAchievements);
    }
  }

  // ---------- finalBossStageTwoFireproof ----------

  @Nested
  class FinalBossStageTwoFireproof {

    @Test
    void update_unrelatedDamageAndBossEvents_staysLocked() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossStageTwoFireproof"));
      AchievementContext breakCompleted = new AchievementContext();
      breakCompleted.bossAchievementId = "finalBossBreakRespected";
      AchievementContext coolinOff = new AchievementContext();
      coolinOff.bossAchievementId = "finalBossStageTwoIceHit";
      AchievementContext snowQueen = new AchievementContext();
      snowQueen.bossAchievementId = "finalBossStageThreeIceHit";

      assertFalse(a.update(new AchievementContext()));
      assertFalse(a.update(damaged()));
      assertFalse(a.update(kill(EnemyType.FINAL_BOSS)));
      assertFalse(a.update(breakCompleted));
      assertFalse(a.update(coolinOff));
      assertFalse(a.update(snowQueen));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_fireproofResult_unlocksOnlyFireproofAndOnlyOnce() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossStageTwoFireproof"));
      Achievement breakAchievement =
          AchievementsFactory.build(finalBossConfig("finalBossBreakRespected"));
      Achievement coolinOff = AchievementsFactory.build(finalBossConfig("finalBossStageTwoIceHit"));
      Achievement snowQueen =
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHit"));
      AchievementContext ctx = new AchievementContext();
      ctx.bossAchievementId = "finalBossStageTwoFireproof";

      assertTrue(a.update(ctx));
      assertTrue(a.isUnlocked());
      assertFalse(a.update(ctx));
      assertFalse(a.update(new AchievementContext()));
      assertTrue(a.isUnlocked());
      assertFalse(breakAchievement.update(ctx));
      assertFalse(coolinOff.update(ctx));
      assertFalse(snowQueen.update(ctx));
      assertFalse(breakAchievement.isUnlocked());
      assertFalse(coolinOff.isUnlocked());
      assertFalse(snowQueen.isUnlocked());
    }

    @Test
    void config_registersOneFireproofAchievementWithExpectedNameAndCondition() {
      AchievementConfig[] configs =
          new Json()
              .fromJson(AchievementConfig[].class, new FileHandle("configs/achievements.json"));
      int fireproofAchievements = 0;
      int fireproofNames = 0;
      for (AchievementConfig c : configs) {
        Achievement a = AchievementsFactory.build(c);
        if ("I'm fireproof!".equals(a.getName())) {
          fireproofNames++;
        }
        if ("finalBoss".equals(c.type)
            && "finalBossStageTwoFireproof".equals(c.bossAchievementId)) {
          fireproofAchievements++;
          assertEquals("I'm fireproof!", a.getName());
          assertFalse(a.update(new AchievementContext()));
          assertFalse(a.update(damaged()));
          AchievementContext ctx = new AchievementContext();
          ctx.bossAchievementId = "finalBossStageTwoFireproof";
          assertTrue(a.update(ctx));
        }
      }
      assertEquals(1, fireproofAchievements);
      assertEquals(1, fireproofNames);
    }
  }

  // ---------- finalBossStageThreeIceHit ----------

  @Nested
  class FinalBossStageThreeIceHit {

    @Test
    void update_unrelatedDamageAndBossEvents_staysLocked() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHit"));
      AchievementContext breakCompleted = new AchievementContext();
      breakCompleted.bossAchievementId = "finalBossBreakRespected";

      assertFalse(a.update(new AchievementContext()));
      assertFalse(a.update(damaged()));
      assertFalse(a.update(kill(EnemyType.FINAL_BOSS)));
      assertFalse(a.update(breakCompleted));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_firstIceHit_unlocksOnlySnowQueenAndOnlyOnce() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHit"));
      Achievement breakAchievement =
          AchievementsFactory.build(finalBossConfig("finalBossBreakRespected"));
      AchievementContext ctx = new AchievementContext();
      ctx.bossAchievementId = "finalBossStageThreeIceHit";

      assertTrue(a.update(ctx));
      assertTrue(a.isUnlocked());
      assertFalse(a.update(ctx));
      assertFalse(breakAchievement.update(ctx));
      assertFalse(breakAchievement.isUnlocked());
    }

    @Test
    void config_registersOneSnowQueenAchievementWithExpectedNameAndCondition() {
      AchievementConfig[] configs =
          new Json()
              .fromJson(AchievementConfig[].class, new FileHandle("configs/achievements.json"));
      int snowQueenAchievements = 0;
      for (AchievementConfig c : configs) {
        Achievement a = AchievementsFactory.build(c);
        if ("finalBoss".equals(c.type) && "finalBossStageThreeIceHit".equals(c.bossAchievementId)) {
          snowQueenAchievements++;
          assertEquals("Whoa! The Snow Queen", a.getName());
          assertFalse(a.update(damaged()));
          AchievementContext ctx = new AchievementContext();
          ctx.bossAchievementId = "finalBossStageThreeIceHit";
          assertTrue(a.update(ctx));
        }
      }
      assertEquals(1, snowQueenAchievements);
    }
  }

  // ---------- finalBossStageThreeIceHitStreak ----------

  @Nested
  class FinalBossStageThreeIceHitStreak {

    @Test
    void update_unrelatedEventsAndRepeatedSingleIceHits_staysLocked() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHitStreak"));
      AchievementContext breakCompleted = new AchievementContext();
      breakCompleted.bossAchievementId = "finalBossBreakRespected";
      AchievementContext coolinOff = new AchievementContext();
      coolinOff.bossAchievementId = "finalBossStageTwoIceHit";
      AchievementContext fireproof = new AchievementContext();
      fireproof.bossAchievementId = "finalBossStageTwoFireproof";
      AchievementContext snowQueen = new AchievementContext();
      snowQueen.bossAchievementId = "finalBossStageThreeIceHit";
      AchievementContext flashedTooFast = new AchievementContext();
      flashedTooFast.bossAchievementId = "finalBossStageThreeMeleeHit";
      AchievementContext statueFlash = new AchievementContext();
      statueFlash.bossAchievementId = "finalBossStageThreeStatueFlash";

      assertFalse(a.isUnlocked());
      assertFalse(a.update(new AchievementContext()));
      assertFalse(a.update(damaged()));
      assertFalse(a.update(kill(EnemyType.FINAL_BOSS)));
      assertFalse(a.update(breakCompleted));
      assertFalse(a.update(coolinOff));
      assertFalse(a.update(fireproof));
      for (int hit = 0; hit < 3; hit++) {
        assertFalse(a.update(snowQueen));
      }
      assertFalse(a.update(flashedTooFast));
      assertFalse(a.update(statueFlash));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_completedIceHitStreak_unlocksOnlyFreezingAndOnlyOnce() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHitStreak"));
      Achievement breakAchievement =
          AchievementsFactory.build(finalBossConfig("finalBossBreakRespected"));
      Achievement coolinOff = AchievementsFactory.build(finalBossConfig("finalBossStageTwoIceHit"));
      Achievement fireproof =
          AchievementsFactory.build(finalBossConfig("finalBossStageTwoFireproof"));
      Achievement snowQueen =
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHit"));
      Achievement flashedTooFast =
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeMeleeHit"));
      Achievement statueFlash =
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeStatueFlash"));
      AchievementContext ctx = new AchievementContext();
      ctx.bossAchievementId = "finalBossStageThreeIceHitStreak";

      assertTrue(a.update(ctx));
      assertTrue(a.isUnlocked());
      assertFalse(a.update(ctx));
      assertFalse(a.update(new AchievementContext()));
      assertTrue(a.isUnlocked());
      assertFalse(breakAchievement.update(ctx));
      assertFalse(coolinOff.update(ctx));
      assertFalse(fireproof.update(ctx));
      assertFalse(snowQueen.update(ctx));
      assertFalse(flashedTooFast.update(ctx));
      assertFalse(statueFlash.update(ctx));
      assertFalse(breakAchievement.isUnlocked());
      assertFalse(coolinOff.isUnlocked());
      assertFalse(fireproof.isUnlocked());
      assertFalse(snowQueen.isUnlocked());
      assertFalse(flashedTooFast.isUnlocked());
      assertFalse(statueFlash.isUnlocked());
    }

    @Test
    void config_registersOneFreezingAchievementWithExpectedNameAndCondition() {
      AchievementConfig[] configs =
          new Json()
              .fromJson(AchievementConfig[].class, new FileHandle("configs/achievements.json"));
      int freezingAchievements = 0;
      int freezingNames = 0;
      for (AchievementConfig c : configs) {
        Achievement a = AchievementsFactory.build(c);
        if ("Brrr, it's freezing!".equals(a.getName())) {
          freezingNames++;
        }
        if ("finalBoss".equals(c.type)
            && "finalBossStageThreeIceHitStreak".equals(c.bossAchievementId)) {
          freezingAchievements++;
          assertEquals("Brrr, it's freezing!", a.getName());
          assertFalse(a.isUnlocked());
          assertFalse(a.update(new AchievementContext()));
          assertFalse(a.update(damaged()));
          AchievementContext ctx = new AchievementContext();
          ctx.bossAchievementId = "finalBossStageThreeIceHitStreak";
          assertTrue(a.update(ctx));
        }
      }
      assertEquals(1, freezingAchievements);
      assertEquals(1, freezingNames);
    }
  }

  // ---------- finalBossStageThreeMeleeHit ----------

  @Nested
  class FinalBossStageThreeMeleeHit {

    @Test
    void update_unrelatedDamageAndBossEvents_staysLocked() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossStageThreeMeleeHit"));
      AchievementContext breakCompleted = new AchievementContext();
      breakCompleted.bossAchievementId = "finalBossBreakRespected";
      AchievementContext coolinOff = new AchievementContext();
      coolinOff.bossAchievementId = "finalBossStageTwoIceHit";
      AchievementContext fireproof = new AchievementContext();
      fireproof.bossAchievementId = "finalBossStageTwoFireproof";
      AchievementContext snowQueen = new AchievementContext();
      snowQueen.bossAchievementId = "finalBossStageThreeIceHit";

      assertFalse(a.update(new AchievementContext()));
      assertFalse(a.update(damaged()));
      assertFalse(a.update(kill(EnemyType.FINAL_BOSS)));
      assertFalse(a.update(breakCompleted));
      assertFalse(a.update(coolinOff));
      assertFalse(a.update(fireproof));
      assertFalse(a.update(snowQueen));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_firstMeleeHit_unlocksOnlyFlashedTooFastAndOnlyOnce() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossStageThreeMeleeHit"));
      Achievement breakAchievement =
          AchievementsFactory.build(finalBossConfig("finalBossBreakRespected"));
      Achievement coolinOff = AchievementsFactory.build(finalBossConfig("finalBossStageTwoIceHit"));
      Achievement fireproof =
          AchievementsFactory.build(finalBossConfig("finalBossStageTwoFireproof"));
      Achievement snowQueen =
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHit"));
      AchievementContext ctx = new AchievementContext();
      ctx.bossAchievementId = "finalBossStageThreeMeleeHit";

      assertTrue(a.update(ctx));
      assertTrue(a.isUnlocked());
      assertFalse(a.update(ctx));
      assertFalse(a.update(new AchievementContext()));
      assertTrue(a.isUnlocked());
      assertFalse(breakAchievement.update(ctx));
      assertFalse(coolinOff.update(ctx));
      assertFalse(fireproof.update(ctx));
      assertFalse(snowQueen.update(ctx));
      assertFalse(breakAchievement.isUnlocked());
      assertFalse(coolinOff.isUnlocked());
      assertFalse(fireproof.isUnlocked());
      assertFalse(snowQueen.isUnlocked());
    }

    @Test
    void config_registersOneFlashedTooFastAchievementWithExpectedNameAndCondition() {
      AchievementConfig[] configs =
          new Json()
              .fromJson(AchievementConfig[].class, new FileHandle("configs/achievements.json"));
      int meleeHitAchievements = 0;
      int flashedTooFastNames = 0;
      for (AchievementConfig c : configs) {
        Achievement a = AchievementsFactory.build(c);
        if ("You flashed too fast".equals(a.getName())) {
          flashedTooFastNames++;
        }
        if ("finalBoss".equals(c.type)
            && "finalBossStageThreeMeleeHit".equals(c.bossAchievementId)) {
          meleeHitAchievements++;
          assertEquals("You flashed too fast", a.getName());
          assertFalse(a.update(new AchievementContext()));
          assertFalse(a.update(damaged()));
          AchievementContext ctx = new AchievementContext();
          ctx.bossAchievementId = "finalBossStageThreeMeleeHit";
          assertTrue(a.update(ctx));
        }
      }
      assertEquals(1, meleeHitAchievements);
      assertEquals(1, flashedTooFastNames);
    }
  }

  // ---------- finalBossStageThreeStatueFlash ----------

  @Nested
  class FinalBossStageThreeStatueFlash {

    @Test
    void update_unrelatedDamageAndBossEvents_staysLocked() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossStageThreeStatueFlash"));
      AchievementContext breakCompleted = new AchievementContext();
      breakCompleted.bossAchievementId = "finalBossBreakRespected";
      AchievementContext coolinOff = new AchievementContext();
      coolinOff.bossAchievementId = "finalBossStageTwoIceHit";
      AchievementContext fireproof = new AchievementContext();
      fireproof.bossAchievementId = "finalBossStageTwoFireproof";
      AchievementContext snowQueen = new AchievementContext();
      snowQueen.bossAchievementId = "finalBossStageThreeIceHit";
      AchievementContext flashedTooFast = new AchievementContext();
      flashedTooFast.bossAchievementId = "finalBossStageThreeMeleeHit";

      assertFalse(a.isUnlocked());
      assertFalse(a.update(new AchievementContext()));
      assertFalse(a.update(damaged()));
      assertFalse(a.update(kill(EnemyType.FINAL_BOSS)));
      assertFalse(a.update(breakCompleted));
      assertFalse(a.update(coolinOff));
      assertFalse(a.update(fireproof));
      assertFalse(a.update(snowQueen));
      assertFalse(a.update(flashedTooFast));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_firstSuccessfulStatueFlash_unlocksOnlyStatueFlashAndOnlyOnce() {
      Achievement a = AchievementsFactory.build(finalBossConfig("finalBossStageThreeStatueFlash"));
      Achievement breakAchievement =
          AchievementsFactory.build(finalBossConfig("finalBossBreakRespected"));
      Achievement coolinOff = AchievementsFactory.build(finalBossConfig("finalBossStageTwoIceHit"));
      Achievement fireproof =
          AchievementsFactory.build(finalBossConfig("finalBossStageTwoFireproof"));
      Achievement snowQueen =
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeIceHit"));
      Achievement flashedTooFast =
          AchievementsFactory.build(finalBossConfig("finalBossStageThreeMeleeHit"));
      AchievementContext ctx = new AchievementContext();
      ctx.bossAchievementId = "finalBossStageThreeStatueFlash";

      assertTrue(a.update(ctx));
      assertTrue(a.isUnlocked());
      assertFalse(a.update(ctx));
      assertFalse(a.update(new AchievementContext()));
      assertTrue(a.isUnlocked());
      assertFalse(breakAchievement.update(ctx));
      assertFalse(coolinOff.update(ctx));
      assertFalse(fireproof.update(ctx));
      assertFalse(snowQueen.update(ctx));
      assertFalse(flashedTooFast.update(ctx));
      assertFalse(breakAchievement.isUnlocked());
      assertFalse(coolinOff.isUnlocked());
      assertFalse(fireproof.isUnlocked());
      assertFalse(snowQueen.isUnlocked());
      assertFalse(flashedTooFast.isUnlocked());
    }

    @Test
    void config_registersOneStatueFlashAchievementWithExpectedNameAndCondition() {
      AchievementConfig[] configs =
          new Json()
              .fromJson(AchievementConfig[].class, new FileHandle("configs/achievements.json"));
      int statueFlashAchievements = 0;
      int statueFlashNames = 0;
      for (AchievementConfig c : configs) {
        Achievement a = AchievementsFactory.build(c);
        if ("How can a statue even flash?".equals(a.getName())) {
          statueFlashNames++;
        }
        if ("finalBoss".equals(c.type)
            && "finalBossStageThreeStatueFlash".equals(c.bossAchievementId)) {
          statueFlashAchievements++;
          assertEquals("How can a statue even flash?", a.getName());
          assertFalse(a.isUnlocked());
          assertFalse(a.update(new AchievementContext()));
          assertFalse(a.update(damaged()));
          AchievementContext ctx = new AchievementContext();
          ctx.bossAchievementId = "finalBossStageThreeStatueFlash";
          assertTrue(a.update(ctx));
        }
      }
      assertEquals(1, statueFlashAchievements);
      assertEquals(1, statueFlashNames);
    }
  }

  // ---------- dungeonClear ----------

  @Nested
  class DungeonClear {

    private Achievement build(String dungeonId) {
      AchievementConfig c = config("dungeonClear");
      c.dungeonId = dungeonId;
      return AchievementsFactory.build(c);
    }

    @Test
    void update_matchingDungeonCompleted_unlocks() {
      Achievement a = build("forest");

      assertTrue(a.update(completed("forest")));
      assertTrue(a.isUnlocked());
    }

    @Test
    void update_differentDungeonCompleted_staysLocked() {
      Achievement a = build("forest");

      assertFalse(a.update(completed("desert")));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_noDungeonCompleted_staysLocked() {
      Achievement a = build("forest");

      assertFalse(a.update(new AchievementContext()));
    }

    @Test
    void update_onlyEnteringTheDungeon_doesNotUnlock() {
      Achievement a = build("forest");

      assertFalse(a.update(entered("forest")));
    }

    @Test
    void update_laterNonMatchingContexts_doNotRelock() {
      Achievement a = build("forest");

      a.update(completed("forest"));
      a.update(completed("desert"));

      assertTrue(a.isUnlocked());
    }

    @Test
    void update_doesNotUseProgress_staysZeroRegardlessOfUnlock() {
      Achievement a = build("forest");

      a.update(completed("forest"));

      assertEquals(0f, a.getProgress());
    }
  }

  // ---------- dungeonReached ----------

  @Nested
  class DungeonReached {

    private Achievement build(String dungeonId) {
      AchievementConfig c = config("dungeonReached");
      c.dungeonId = dungeonId;
      return AchievementsFactory.build(c);
    }

    @Test
    void update_matchingDungeonEntered_unlocks() {
      Achievement a = build("cave");

      assertTrue(a.update(entered("cave")));
      assertTrue(a.isUnlocked());
    }

    @Test
    void update_differentDungeonEntered_staysLocked() {
      Achievement a = build("cave");

      assertFalse(a.update(entered("forest")));
    }

    @Test
    void update_noDungeonEntered_staysLocked() {
      Achievement a = build("cave");

      assertFalse(a.update(new AchievementContext()));
    }

    @Test
    void update_completingTheDungeonWithoutEntering_doesNotUnlock() {
      Achievement a = build("cave");

      assertFalse(a.update(completed("cave")));
    }
  }

  // ---------- gold ----------

  @Nested
  class Gold {

    private Achievement build(float target) {
      AchievementConfig c = config("gold");
      c.target = target;
      return AchievementsFactory.build(c);
    }

    @Test
    void update_firstReadingAlone_establishesBaselineWithoutUnlocking() {
      Achievement a = build(100);

      // The first reading becomes the baseline; progress against it starts at 0.
      assertFalse(a.update(gold(100)));
      assertEquals(0f, a.getProgress());
    }

    @Test
    void update_earningGoldAfterBaseline_tracksTheDelta() {
      Achievement a = build(100);

      a.update(gold(50)); // baseline = 50
      a.update(gold(90)); // earned 40 so far

      assertEquals(40f, a.getProgress());
    }

    @Test
    void update_goldReachesTargetAboveBaseline_unlocks() {
      Achievement a = build(100);

      a.update(gold(0)); // baseline = 0
      assertTrue(a.update(gold(100))); // earned 100, matches target
    }

    @Test
    void update_goldAboveTargetAboveBaseline_unlocks() {
      Achievement a = build(100);

      a.update(gold(0));
      assertTrue(a.update(gold(500)));
    }

    @Test
    void update_startingWithExistingGold_onlyCountsFurtherEarnings() {
      Achievement a = build(100);

      a.update(gold(50)); // player started with 50; this becomes the baseline, not progress
      assertFalse(a.update(gold(120))); // earned 70 so far, short of 100
      assertTrue(a.update(gold(150))); // earned 100, unlocks
    }

    @Test
    void update_nullGoldTotal_staysLockedWithoutThrowing() {
      Achievement a = build(100);

      assertFalse(a.update(gold(null)));
    }

    @Test
    void update_nullReadingBeforeBaseline_doesNotEstablishABaseline() {
      Achievement a = build(100);

      a.update(gold(null)); // ignored; no baseline set yet
      a.update(gold(0)); // this becomes the baseline instead

      assertTrue(a.update(gold(100))); // earned 100 relative to the real baseline
    }

    @Test
    void update_goldReachesTargetOverSeveralUpdates_unlocksOnceTargetIsReached() {
      Achievement a = build(100);

      a.update(gold(0)); // baseline
      assertFalse(a.update(gold(40)));
      assertFalse(a.update(gold(80)));
      assertTrue(a.update(gold(120)));
    }
  }

  // ---------- enemyKillCount ----------

  @Nested
  class EnemyKillCount {

    private Achievement build(float target, EnemyType type) {
      AchievementConfig c = config("enemyKillCount");
      c.target = target;
      c.enemyType = type;
      return AchievementsFactory.build(c);
    }

    @Test
    void update_noEnemyTypeFilter_anyEnemyKillsCountTowardsTarget() {
      Achievement a = build(3, null);

      assertFalse(a.update(kill(EnemyType.ZOMBIE)));
      assertFalse(a.update(kill(EnemyType.KNIGHT)));
      assertTrue(a.update(kill(EnemyType.SNAKE_MINI_BOSS)));
    }

    @Test
    void update_withEnemyTypeFilter_onlyMatchingKillsCount() {
      Achievement a = build(2, EnemyType.ZOMBIE);

      assertFalse(a.update(kill(EnemyType.ZOMBIE)));
      assertFalse(a.update(kill(EnemyType.KNIGHT)));
      assertFalse(a.update(kill(EnemyType.KNIGHT)));
      assertTrue(a.update(kill(EnemyType.ZOMBIE)));
    }

    @Test
    void update_withEnemyTypeFilter_nonMatchingKillsNeverUnlock() {
      Achievement a = build(1, EnemyType.ZOMBIE);

      assertFalse(a.update(kill(EnemyType.KNIGHT)));
      assertFalse(a.update(kill(EnemyType.SNAKE_MINI_BOSS)));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_contextWithoutKill_doesNotCount() {
      Achievement a = build(1, null);

      assertFalse(a.update(new AchievementContext()));
      assertFalse(a.update(completed("forest")));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_targetOfOne_unlocksOnFirstKill() {
      Achievement a = build(1, null);

      assertTrue(a.update(kill(EnemyType.KNIGHT)));
    }

    @Test
    void update_playerDamaged_doesNotResetKillCount() {
      Achievement a = build(3, null);

      a.update(kill(EnemyType.ZOMBIE));
      a.update(kill(EnemyType.ZOMBIE));
      a.update(damaged());

      assertTrue(a.update(kill(EnemyType.ZOMBIE)));
    }

    @Test
    void update_killTogetherWithPlayerDamaged_stillCounts() {
      Achievement a = build(1, null);
      AchievementContext ctx = kill(EnemyType.ZOMBIE);
      ctx.playerDamaged = true;

      assertTrue(a.update(ctx));
    }

    @Test
    void build_twoAchievementsFromSameConfig_keepIndependentCounts() {
      AchievementConfig c = config("enemyKillCount");
      c.target = 2;
      Achievement first = AchievementsFactory.build(c);
      Achievement second = AchievementsFactory.build(c);

      first.update(kill(EnemyType.ZOMBIE));
      first.update(kill(EnemyType.ZOMBIE));

      assertTrue(first.isUnlocked());
      assertFalse(second.update(kill(EnemyType.ZOMBIE)));
      assertFalse(second.isUnlocked());
    }

    @Test
    void update_eachMatchingKill_incrementsProgressByOne() {
      Achievement a = build(5, null);

      a.update(kill(EnemyType.ZOMBIE));
      assertEquals(1f, a.getProgress());

      a.update(kill(EnemyType.KNIGHT));
      assertEquals(2f, a.getProgress());
    }

    @Test
    void update_nonMatchingKills_doNotIncrementProgress() {
      Achievement a = build(5, EnemyType.ZOMBIE);

      a.update(kill(EnemyType.KNIGHT));
      a.update(kill(EnemyType.SNAKE_MINI_BOSS));

      assertEquals(0f, a.getProgress());
    }

    @Test
    void update_progressStopsIncrementingOnceUnlocked() {
      Achievement a = build(1, null);

      a.update(kill(EnemyType.ZOMBIE)); // unlocks here
      a.update(kill(EnemyType.KNIGHT)); // should be a no-op: already unlocked

      assertEquals(1f, a.getProgress());
    }
  }

  // ---------- killStreak ----------

  @Nested
  class KillStreak {

    private Achievement build(float target) {
      AchievementConfig c = config("killStreak");
      c.target = target;
      return AchievementsFactory.build(c);
    }

    @Test
    void update_consecutiveKillsReachTarget_unlocks() {
      Achievement a = build(3);

      assertFalse(a.update(kill(EnemyType.ZOMBIE)));
      assertFalse(a.update(kill(EnemyType.KNIGHT)));
      assertTrue(a.update(kill(EnemyType.SNAKE_MINI_BOSS)));
    }

    @Test
    void update_playerDamagedMidStreak_resetsStreak() {
      Achievement a = build(3);

      a.update(kill(EnemyType.ZOMBIE));
      a.update(kill(EnemyType.ZOMBIE));
      a.update(damaged());

      // Streak restarted, so two more kills are not enough
      assertFalse(a.update(kill(EnemyType.ZOMBIE)));
      assertFalse(a.update(kill(EnemyType.ZOMBIE)));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_streakRestartsAfterDamage_unlocksOnceFullStreakIsAchieved() {
      Achievement a = build(2);

      a.update(kill(EnemyType.ZOMBIE));
      a.update(damaged());
      a.update(kill(EnemyType.ZOMBIE));

      assertTrue(a.update(kill(EnemyType.ZOMBIE)));
    }

    @Test
    void update_killInSameContextAsDamage_resetsStreakAndKillDoesNotCount() {
      Achievement a = build(2);
      AchievementContext killWhileDamaged = kill(EnemyType.ZOMBIE);
      killWhileDamaged.playerDamaged = true;

      a.update(kill(EnemyType.ZOMBIE));
      assertFalse(a.update(killWhileDamaged));

      // Streak is back to zero, so a single kill is not enough
      assertFalse(a.update(kill(EnemyType.ZOMBIE)));
    }

    @Test
    void update_contextWithoutKillOrDamage_doesNotBreakOrAdvanceStreak() {
      Achievement a = build(2);

      a.update(kill(EnemyType.ZOMBIE));
      assertFalse(a.update(completed("forest")));
      assertTrue(a.update(kill(EnemyType.ZOMBIE)));
    }

    @Test
    void update_damageBeforeAnyKills_doesNotUnlock() {
      Achievement a = build(1);

      assertFalse(a.update(damaged()));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_targetOfOne_unlocksOnFirstKill() {
      Achievement a = build(1);

      assertTrue(a.update(kill(EnemyType.KNIGHT)));
    }

    @Test
    void build_twoAchievementsFromSameConfig_keepIndependentStreaks() {
      AchievementConfig c = config("killStreak");
      c.target = 2;
      Achievement first = AchievementsFactory.build(c);
      Achievement second = AchievementsFactory.build(c);

      first.update(kill(EnemyType.ZOMBIE));
      first.update(kill(EnemyType.ZOMBIE));

      assertTrue(first.isUnlocked());
      assertFalse(second.update(kill(EnemyType.ZOMBIE)));
    }

    @Test
    void update_eachKillInStreak_incrementsProgress() {
      Achievement a = build(5);

      a.update(kill(EnemyType.ZOMBIE));
      assertEquals(1f, a.getProgress());

      a.update(kill(EnemyType.KNIGHT));
      assertEquals(2f, a.getProgress());
    }

    @Test
    void update_playerDamaged_resetsProgressToZero() {
      Achievement a = build(5);

      a.update(kill(EnemyType.ZOMBIE));
      a.update(kill(EnemyType.ZOMBIE));
      a.update(damaged());

      assertEquals(0f, a.getProgress());
    }

    @Test
    void update_progressRebuildsAfterReset() {
      Achievement a = build(5);

      a.update(kill(EnemyType.ZOMBIE));
      a.update(damaged());
      a.update(kill(EnemyType.ZOMBIE));
      a.update(kill(EnemyType.ZOMBIE));

      assertEquals(2f, a.getProgress());
    }
  }

  // ---------- speedRun ----------

  @Nested
  class SpeedRun {

    private Achievement build(String dungeonId, float maxSeconds) {
      AchievementConfig c = config("speedRun");
      c.dungeonId = dungeonId;
      c.maxSeconds = maxSeconds;
      return AchievementsFactory.build(c);
    }

    @Test
    void update_completedUnderTimeLimit_unlocks() {
      Achievement a = build("forest", 60f);

      a.update(timing("forest", 45f));

      assertTrue(a.update(completed("forest")));
      assertTrue(a.isUnlocked());
    }

    @Test
    void update_completedExactlyAtTimeLimit_unlocks() {
      Achievement a = build("forest", 60f);

      a.update(timing("forest", 60f));

      assertTrue(a.update(completed("forest")));
    }

    @Test
    void update_completedOverTimeLimit_staysLocked() {
      Achievement a = build("forest", 60f);

      a.update(timing("forest", 60.5f));

      assertFalse(a.update(completed("forest")));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_completedWithoutAnyTimingUpdate_staysLocked() {
      Achievement a = build("forest", 60f);

      assertFalse(a.update(completed("forest")));
    }

    @Test
    void update_timingUpdateAloneWithoutCompletion_doesNotUnlock() {
      Achievement a = build("forest", 60f);

      assertFalse(a.update(timing("forest", 10f)));
    }

    @Test
    void update_differentDungeonCompleted_staysLocked() {
      Achievement a = build("forest", 60f);

      a.update(timing("forest", 10f));

      assertFalse(a.update(completed("desert")));
    }

    @Test
    void update_timingFromDifferentDungeon_isIgnored() {
      Achievement a = build("forest", 60f);

      a.update(timing("desert", 10f));

      // No forest time was ever recorded, so completing forest cannot unlock
      assertFalse(a.update(completed("forest")));
    }

    @Test
    void update_zeroSeconds_isIgnoredAsATimeReading() {
      Achievement a = build("forest", 60f);

      a.update(timing("forest", 0f));

      assertFalse(a.update(completed("forest")));
    }

    @Test
    void update_negativeSeconds_isIgnoredAsATimeReading() {
      Achievement a = build("forest", 60f);

      a.update(timing("forest", -5f));

      assertFalse(a.update(completed("forest")));
    }

    @Test
    void update_usesMostRecentTimeReading() {
      Achievement a = build("forest", 60f);

      a.update(timing("forest", 20f));
      a.update(timing("forest", 90f));

      // Latest reading (90s) is over the limit even though an earlier one was under
      assertFalse(a.update(completed("forest")));
    }

    @Test
    void update_laterFasterReadingReplacesEarlierSlowerOne() {
      Achievement a = build("forest", 60f);

      a.update(timing("forest", 90f));
      a.update(timing("forest", 30f));

      assertTrue(a.update(completed("forest")));
    }

    @Test
    void update_completionContextCarryingItsOwnTime_usesThatTime() {
      Achievement a = build("forest", 60f);
      AchievementContext ctx = completed("forest");
      ctx.dungeonId = "forest";
      ctx.dungeonSeconds = 42f;

      assertTrue(a.update(ctx));
    }

    @Test
    void update_completionContextCarryingSlowTime_overridesEarlierFastTime() {
      Achievement a = build("forest", 60f);
      a.update(timing("forest", 10f));
      AchievementContext ctx = completed("forest");
      ctx.dungeonId = "forest";
      ctx.dungeonSeconds = 120f;

      assertFalse(a.update(ctx));
    }

    @Test
    void build_twoAchievementsFromSameConfig_keepIndependentTimes() {
      AchievementConfig c = config("speedRun");
      c.dungeonId = "forest";
      c.maxSeconds = 60f;
      Achievement first = AchievementsFactory.build(c);
      Achievement second = AchievementsFactory.build(c);

      first.update(timing("forest", 10f));

      assertTrue(first.update(completed("forest")));
      assertFalse(second.update(completed("forest")));
    }

    @Test
    void update_timingReading_isStoredInProgress() {
      Achievement a = build("forest", 60f);

      a.update(timing("forest", 37f));

      assertEquals(37f, a.getProgress());
    }

    @Test
    void update_laterReadingOverwritesProgress() {
      Achievement a = build("forest", 60f);

      a.update(timing("forest", 20f));
      a.update(timing("forest", 90f));

      assertEquals(90f, a.getProgress());
    }

    @Test
    void update_zeroOrNegativeReadings_doNotOverwriteProgress() {
      Achievement a = build("forest", 60f);

      a.update(timing("forest", 25f));
      a.update(timing("forest", 0f));
      a.update(timing("forest", -5f));

      assertEquals(25f, a.getProgress());
    }
  }

  // ---------- enemySet ----------

  @Nested
  class EnemySet {

    private Achievement build(EnemyType... types) {
      AchievementConfig c = config("enemySet");
      c.enemyTypes = List.of(types);
      return AchievementsFactory.build(c);
    }

    @Test
    void update_allRequiredTypesKilled_unlocks() {
      Achievement a = build(EnemyType.ZOMBIE, EnemyType.KNIGHT);

      assertFalse(a.update(kill(EnemyType.ZOMBIE)));
      assertTrue(a.update(kill(EnemyType.KNIGHT)));
      assertTrue(a.isUnlocked());
    }

    @Test
    void update_requiredTypesKilledInReverseOrder_stillUnlocks() {
      Achievement a = build(EnemyType.ZOMBIE, EnemyType.KNIGHT);

      assertFalse(a.update(kill(EnemyType.KNIGHT)));
      assertTrue(a.update(kill(EnemyType.ZOMBIE)));
    }

    @Test
    void update_onlySomeRequiredTypesKilled_staysLocked() {
      Achievement a = build(EnemyType.ZOMBIE, EnemyType.KNIGHT, EnemyType.SNAKE_MINI_BOSS);

      a.update(kill(EnemyType.ZOMBIE));
      a.update(kill(EnemyType.KNIGHT));

      assertFalse(a.isUnlocked());
    }

    @Test
    void update_repeatedKillsOfSameType_doNotSatisfyOtherTypes() {
      Achievement a = build(EnemyType.ZOMBIE, EnemyType.KNIGHT);

      assertFalse(a.update(kill(EnemyType.ZOMBIE)));
      assertFalse(a.update(kill(EnemyType.ZOMBIE)));
      assertFalse(a.update(kill(EnemyType.ZOMBIE)));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_killOfTypeOutsideTheSet_isIgnored() {
      Achievement a = build(EnemyType.ZOMBIE, EnemyType.KNIGHT);

      a.update(kill(EnemyType.SNAKE_MINI_BOSS));
      a.update(kill(EnemyType.ZOMBIE));

      assertFalse(a.isUnlocked());
      assertTrue(a.update(kill(EnemyType.KNIGHT)));
    }

    @Test
    void update_contextWithoutKill_isIgnored() {
      Achievement a = build(EnemyType.ZOMBIE);

      assertFalse(a.update(new AchievementContext()));
      assertFalse(a.update(damaged()));
      assertFalse(a.isUnlocked());
    }

    @Test
    void update_singleTypeSet_unlocksOnThatKill() {
      Achievement a = build(EnemyType.SNAKE_MINI_BOSS);

      assertTrue(a.update(kill(EnemyType.SNAKE_MINI_BOSS)));
    }

    @Test
    void update_playerDamagedBetweenKills_doesNotResetProgress() {
      Achievement a = build(EnemyType.ZOMBIE, EnemyType.KNIGHT);

      a.update(kill(EnemyType.ZOMBIE));
      a.update(damaged());

      assertTrue(a.update(kill(EnemyType.KNIGHT)));
    }

    @Test
    void build_twoAchievementsFromSameConfig_trackSeenTypesIndependently() {
      AchievementConfig c = config("enemySet");
      c.enemyTypes = List.of(EnemyType.ZOMBIE, EnemyType.KNIGHT);
      Achievement first = AchievementsFactory.build(c);
      Achievement second = AchievementsFactory.build(c);

      first.update(kill(EnemyType.ZOMBIE));
      first.update(kill(EnemyType.KNIGHT));

      assertTrue(first.isUnlocked());
      assertFalse(second.update(kill(EnemyType.KNIGHT)));
      assertFalse(second.isUnlocked());
    }

    @Test
    void update_tracksSeenTypesViaItsOwnSet_notTheSharedProgressField() {
      Achievement a = build(EnemyType.ZOMBIE, EnemyType.KNIGHT);

      a.update(kill(EnemyType.ZOMBIE));
      a.update(kill(EnemyType.KNIGHT));

      // enemySet tracks seen types in its own closure-local Set, not via addProgress.
      assertEquals(0f, a.getProgress());
    }
  }
}
