package com.csse3200.game.components.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  private static AchievementContext bossEvent(String bossAchievementId) {
    AchievementContext ctx = new AchievementContext();
    ctx.bossAchievementId = bossAchievementId;
    return ctx;
  }

  private static AchievementConfig[] loadConfigs() {
    return new Json()
        .fromJson(AchievementConfig[].class, new FileHandle("configs/achievements.json"));
  }

  // ---------- shared data ----------

  private record Boss(String id, String name) {}

  private static final List<Boss> BOSSES =
      List.of(
          new Boss("finalBossBreakRespected", "I need a break too"),
          new Boss("finalBossStageTwoIceHit", "Coolin' off, make it fun"),
          new Boss("finalBossStageTwoFireproof", "I'm fireproof!"),
          new Boss("finalBossStageThreeIceHit", "Whoa! The Snow Queen"),
          new Boss("finalBossStageThreeIceHitStreak", "Brrr, it's freezing!"),
          new Boss("finalBossStageThreeMeleeHit", "You flashed too fast"),
          new Boss("finalBossStageThreeStatueFlash", "How can a statue even flash?"));

  /** One valid config for every achievement type, keyed by type name. Fresh objects each call. */
  private static Map<String, AchievementConfig> everyType() {
    AchievementConfig dungeon = config("dungeonClear");
    dungeon.dungeonId = "d";
    AchievementConfig reached = config("dungeonReached");
    reached.dungeonId = "d";
    AchievementConfig speed = config("speedRun");
    speed.dungeonId = "d";
    AchievementConfig set = config("enemySet");
    set.enemyTypes = List.of(EnemyType.KNIGHT);

    Map<String, AchievementConfig> all = new LinkedHashMap<>();
    all.put("dungeonClear", dungeon);
    all.put("dungeonReached", reached);
    all.put("gold", config("gold"));
    all.put("enemyKillCount", config("enemyKillCount"));
    all.put("killStreak", config("killStreak"));
    all.put("speedRun", speed);
    all.put("enemySet", set);
    all.put("finalBoss", finalBossConfig("finalBossBreakRespected"));
    return all;
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
    void build_missingDescription_leavesDescriptionNull() {
      assertNull(AchievementsFactory.build(config("gold")).getDescription());
    }

    @Test
    void build_everyType_keepsNameAndDescriptionAndStartsLocked() {
      for (Map.Entry<String, AchievementConfig> entry : everyType().entrySet()) {
        String type = entry.getKey();
        AchievementConfig c = entry.getValue();
        c.name = "name-" + type;
        c.description = "desc-" + type;

        Achievement a = AchievementsFactory.build(c);

        assertEquals("name-" + type, a.getName(), type);
        assertEquals("desc-" + type, a.getDescription(), type);
        assertFalse(a.isUnlocked(), type);
      }
    }

    @Test
    void build_everyType_startsWithExpectedProgress() {
      for (Map.Entry<String, AchievementConfig> entry : everyType().entrySet()) {
        String type = entry.getKey();
        // speedRun uses MAX_VALUE as its "no time recorded yet" sentinel; everything else starts
        // at 0
        float expected = "speedRun".equals(type) ? Float.MAX_VALUE : 0f;

        assertEquals(expected, AchievementsFactory.build(entry.getValue()).getProgress(), type);
      }
    }

    @Test
    void build_countingTypes_setTargetForTheUi() {
      Map<String, Float> targets = new LinkedHashMap<>();
      targets.put("gold", 60f);
      targets.put("enemyKillCount", 5f);
      targets.put("killStreak", 7f);

      for (Map.Entry<String, Float> entry : targets.entrySet()) {
        AchievementConfig c = config(entry.getKey());
        c.target = entry.getValue();

        assertEquals(entry.getValue(), AchievementsFactory.build(c).getTarget(), entry.getKey());
      }
    }

    @Test
    void constructor_isPrivateAndThrowsIllegalStateException() throws Exception {
      Constructor<AchievementsFactory> constructor =
          AchievementsFactory.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      InvocationTargetException e =
          assertThrows(InvocationTargetException.class, constructor::newInstance);

      assertTrue(e.getCause() instanceof IllegalStateException);
    }
  }

  // ---------- finalBoss (one table drives all seven achievements) ----------

  @Test
  void finalBoss_unlocksOnlyOnItsOwnIdAndOnlyOnce() {
    for (Boss boss : BOSSES) {
      String id = boss.id();
      Achievement a = AchievementsFactory.build(finalBossConfig(id));

      assertTrue(a.update(bossEvent(id)), id);
      assertTrue(a.isUnlocked(), id);
      assertFalse(a.update(bossEvent(id)), id);
      assertFalse(a.update(new AchievementContext()), id);
      assertTrue(a.isUnlocked(), id);
    }
  }

  @Test
  void finalBoss_ignoresEveryOtherBossIdAndUnrelatedEvents() {
    for (Boss boss : BOSSES) {
      String id = boss.id();
      Achievement a = AchievementsFactory.build(finalBossConfig(id));

      assertFalse(a.update(new AchievementContext()), id);
      assertFalse(a.update(damaged()), id);
      assertFalse(a.update(kill(EnemyType.FINAL_BOSS)), id);
      assertFalse(a.update(entered("finalDungeon")), id);
      assertFalse(a.update(completed("finalDungeon")), id);
      for (String unrelated : List.of("", "unknownFinalBossAchievement", id + "Typo")) {
        assertFalse(a.update(bossEvent(unrelated)), id + " vs " + unrelated);
      }
      for (Boss other : BOSSES) {
        if (!other.id().equals(id)) {
          assertFalse(a.update(bossEvent(other.id())), id + " vs " + other.id());
        }
      }
      assertFalse(a.isUnlocked(), id);
    }
  }

  @Test
  void finalBoss_configRegistersEachBossOnceWithTheExpectedName() {
    AchievementConfig[] configs = loadConfigs();

    for (Boss boss : BOSSES) {
      String id = boss.id();
      int found = 0;
      for (AchievementConfig c : configs) {
        if ("finalBoss".equals(c.type) && id.equals(c.bossAchievementId)) {
          found++;
          Achievement a = AchievementsFactory.build(c);
          assertEquals(boss.name(), a.getName(), id);
          assertFalse(a.update(new AchievementContext()), id);
          assertFalse(a.update(damaged()), id);
          assertTrue(a.update(bossEvent(id)), id);
        }
      }
      assertEquals(1, found, id);
    }
  }

  @Test
  void finalBoss_configRegistersExactlyTheKnownBossIds() {
    Set<String> expected = BOSSES.stream().map(Boss::id).collect(Collectors.toSet());
    Set<String> actual =
        Arrays.stream(loadConfigs())
            .filter(c -> "finalBoss".equals(c.type))
            .map(c -> c.bossAchievementId)
            .collect(Collectors.toSet());

    assertEquals(expected, actual);
  }

  // ---------- whole config file ----------

  @Test
  void config_everyEntryBuildsAndHasANameAndDescription() {
    for (AchievementConfig c : loadConfigs()) {
      Achievement a = AchievementsFactory.build(c); // throws on unknown type or missing fields

      assertFalse(a.getName() == null || a.getName().isBlank(), c.type);
      assertFalse(a.getDescription() == null || a.getDescription().isBlank(), a.getName());
    }
  }

  @Test
  void config_achievementNamesAreUnique() {
    Set<String> seen = new HashSet<>();
    for (AchievementConfig c : loadConfigs()) {
      assertTrue(seen.add(c.name), "Duplicate achievement name: " + c.name);
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
    void update_goldEarnedBelowTarget_staysLocked() {
      for (int reading : new int[] {0, 40, 99}) {
        Achievement a = build(100);

        a.update(gold(0)); // baseline

        assertFalse(a.update(gold(reading)), "reading " + reading);
      }
    }

    @Test
    void update_goldEarnedAtOrAboveTarget_unlocks() {
      for (int reading : new int[] {100, 500}) {
        Achievement a = build(100);

        a.update(gold(0)); // baseline

        assertTrue(a.update(gold(reading)), "reading " + reading);
      }
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
    void update_completedWithTimeWithinTheLimit_unlocks() {
      for (float seconds : new float[] {1f, 45f, 60f}) {
        Achievement a = build("forest", 60f);

        a.update(timing("forest", seconds));

        assertTrue(a.update(completed("forest")), "time " + seconds);
        assertTrue(a.isUnlocked(), "time " + seconds);
      }
    }

    @Test
    void update_completedWithTimeOverTheLimit_staysLocked() {
      for (float seconds : new float[] {60.5f, 90f}) {
        Achievement a = build("forest", 60f);

        a.update(timing("forest", seconds));

        assertFalse(a.update(completed("forest")), "time " + seconds);
        assertFalse(a.isUnlocked(), "time " + seconds);
      }
    }

    @Test
    void update_zeroOrNegativeReading_isIgnoredAsATimeReading() {
      for (float seconds : new float[] {0f, -5f}) {
        Achievement a = build("forest", 60f);

        a.update(timing("forest", seconds));

        assertFalse(a.update(completed("forest")), "reading " + seconds);
      }
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
