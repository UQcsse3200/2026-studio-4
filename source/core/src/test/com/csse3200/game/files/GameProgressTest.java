package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.GameProgress.SaveData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class GameProgressTest {

  @BeforeEach
  void resetSave() {
    GameProgress.clearSave();
    GameProgress.clearAchievements();
  }

  @Test
  void shouldFormatTimeAsMinutesAndSeconds() {
    assertEquals("0:00", GameProgress.formatTime(0L));
    assertEquals("0:05", GameProgress.formatTime(5_000L));
    assertEquals("1:05", GameProgress.formatTime(65_000L));
  }

  @Test
  void shouldRecordLastAndBestRunTimes() {
    GameProgress.recordRun(90_000L);
    SaveData first = GameProgress.get();
    assertEquals(90_000L, first.lastRunMs);
    assertEquals(90_000L, first.bestRunMs);

    GameProgress.recordRun(30_000L);
    SaveData faster = GameProgress.get();
    assertEquals(30_000L, faster.lastRunMs);
    assertEquals(30_000L, faster.bestRunMs);

    GameProgress.recordRun(45_000L);
    SaveData slower = GameProgress.get();
    assertEquals(45_000L, slower.lastRunMs);
    assertEquals(30_000L, slower.bestRunMs);
  }

  @Test
  void shouldUnlockAchievementsOnceAndClearThem() {
    GameProgress.unlock("first-blood");
    GameProgress.unlock("first-blood");
    GameProgress.unlock("boss");
    assertEquals(2, GameProgress.get().achievements.size());
    assertTrue(GameProgress.get().achievements.contains("boss"));

    GameProgress.clearAchievements();
    assertTrue(GameProgress.get().achievements.isEmpty());
  }

  @Test
  void shouldClearSaveTimesWithoutDroppingAchievements() {
    GameProgress.recordRun(12_000L);
    GameProgress.unlock("timer");
    GameProgress.clearSave();
    SaveData data = GameProgress.get();
    assertEquals(0L, data.lastRunMs);
    assertEquals(0L, data.bestRunMs);
    assertEquals(1, data.achievements.size());
  }

  @Test
  void shouldIgnoreBlankAchievementIdsAndNegativeTimes() {
    GameProgress.unlock(null);
    GameProgress.unlock("");
    GameProgress.unlock("   ");
    assertTrue(GameProgress.get().achievements.isEmpty());

    GameProgress.recordRun(-1_500L);
    SaveData data = GameProgress.get();
    assertEquals(0L, data.lastRunMs);
    assertEquals(0L, data.bestRunMs);
    assertEquals("0:00", GameProgress.formatTime(-1_500L));
  }

  @Test
  void shouldClearAchievementsWithoutDroppingTimes() {
    GameProgress.recordRun(20_000L);
    GameProgress.unlock("clear");
    GameProgress.clearAchievements();
    SaveData data = GameProgress.get();
    assertEquals(20_000L, data.lastRunMs);
    assertEquals(20_000L, data.bestRunMs);
    assertTrue(data.achievements.isEmpty());
  }
}
