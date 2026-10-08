package com.csse3200.game.files;

import com.csse3200.game.files.FileLoader.Location;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Persist last/best run time and unlocked achievements next to user settings. */
public class GameProgress {
  private static final String ROOT_DIR = "DECO2800Game";
  private static final String SAVE_FILE = "game-save.json";

  /**
   * @return copy of the on-disk save, or defaults if none exists
   */
  public static SaveData get() {
    String path = ROOT_DIR + File.separator + SAVE_FILE;
    SaveData data = FileLoader.readClass(SaveData.class, path, Location.EXTERNAL);
    if (data == null) {
      return new SaveData();
    }
    if (data.achievements == null) {
      data.achievements = new ArrayList<>();
    }
    return data;
  }

  /** Writes the save file. */
  public static void set(SaveData data) {
    if (data.achievements == null) {
      data.achievements = new ArrayList<>();
    }
    String path = ROOT_DIR + File.separator + SAVE_FILE;
    FileLoader.writeClass(data, path, Location.EXTERNAL);
  }

  /** Clears last/best times. Achievements are left as-is. */
  public static void clearSave() {
    SaveData data = get();
    data.lastRunMs = 0L;
    data.bestRunMs = 0L;
    set(data);
  }

  /** Clears unlocked achievements. Times are left as-is. */
  public static void clearAchievements() {
    SaveData data = get();
    data.achievements = new ArrayList<>();
    set(data);
  }

  /** Records a finished run time and keeps the best. */
  public static void recordRun(long elapsedMs) {
    SaveData data = get();
    long safe = Math.max(0L, elapsedMs);
    data.lastRunMs = safe;
    if (data.bestRunMs <= 0L || safe < data.bestRunMs) {
      data.bestRunMs = safe;
    }
    set(data);
  }

  /** Unlocks an achievement id if it is not already present. */
  public static void unlock(String id) {
    if (id == null || id.isBlank()) {
      return;
    }
    SaveData data = get();
    if (!data.achievements.contains(id)) {
      data.achievements.add(id);
      set(data);
    }
  }

  /** Formats a millisecond duration as m:ss. */
  public static String formatTime(long elapsedMs) {
    long totalSeconds = Math.max(0L, elapsedMs) / 1000L;
    long minutes = totalSeconds / 60L;
    long seconds = totalSeconds % 60L;
    return String.format("%d:%02d", minutes, seconds);
  }

  /** On-disk save payload. */
  public static class SaveData {
    public long lastRunMs;
    public long bestRunMs;
    public List<String> achievements = new ArrayList<>();
  }

  private GameProgress() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
