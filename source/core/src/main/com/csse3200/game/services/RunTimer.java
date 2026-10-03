package com.csse3200.game.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class RunTimer {
  private final GameTime gameTime;
  private float totalTime;
  private float dungeonTime;
  private final Map<String, Float> dungeonTimes = new LinkedHashMap<>();
  private String currentDungeonId;
  private boolean runRunning;
  private boolean dungeonRunning;

  public RunTimer(GameTime gameTime) {
    this.gameTime = gameTime;
  }

  public void startRun() {
    if (!runRunning) {
      runRunning = true;
      totalTime = 0;
      dungeonTime = 0f;
      currentDungeonId = null;
      dungeonRunning = false;
      dungeonTimes.clear();
    }
  }

  /** Restores an elapsed run and continues counting from that time. */
  public void restoreRun(float elapsedSeconds) {
    restoreRun(elapsedSeconds, null);
  }

  /** Restores elapsed run and per-dungeon times, then continues counting the run. */
  public void restoreRun(float elapsedSeconds, Map<String, Float> savedDungeonTimes) {
    if (!Float.isFinite(elapsedSeconds) || elapsedSeconds < 0f) {
      throw new IllegalArgumentException("Elapsed run time must be a finite non-negative value");
    }
    totalTime = elapsedSeconds;
    dungeonTimes.clear();
    if (savedDungeonTimes != null) {
      savedDungeonTimes.forEach(
          (id, seconds) -> {
            if (id == null
                || id.isBlank()
                || seconds == null
                || !Float.isFinite(seconds)
                || seconds < 0f) {
              throw new IllegalArgumentException("Invalid saved dungeon time");
            }
            dungeonTimes.put(id, seconds);
          });
    }
    dungeonTime = 0f;
    currentDungeonId = null;
    dungeonRunning = false;
    runRunning = true;
  }

  /** Registers a dungeon so unvisited dungeons can also be displayed with zero time. */
  public void registerDungeon(String dungeonId) {
    if (dungeonId != null && !dungeonId.isBlank()) {
      dungeonTimes.putIfAbsent(dungeonId, 0f);
    }
  }

  public void startDungeon(String dungeonId) {
    if (dungeonId == null || dungeonId.isBlank()) {
      stopDungeon();
      return;
    }
    if (dungeonRunning && Objects.equals(currentDungeonId, dungeonId)) {
      return;
    }
    stopDungeon();
    registerDungeon(dungeonId);
    currentDungeonId = dungeonId;
    dungeonTime = dungeonTimes.get(dungeonId);
    dungeonRunning = true;
  }

  /** Call once per frame. Uses scaled delta so pausing via timeScale pauses the timers. */
  public void update() {
    float delta = gameTime.getDeltaTime();
    if (runRunning) totalTime += delta;
    if (dungeonRunning) {
      dungeonTime += delta;
      dungeonTimes.put(currentDungeonId, dungeonTime);
    }
  }

  public void stopDungeon() {
    if (dungeonRunning && currentDungeonId != null) {
      dungeonTimes.put(currentDungeonId, dungeonTime);
    }
    dungeonRunning = false;
    currentDungeonId = null;
  }

  public void stopRun() {
    runRunning = false;
    stopDungeon();
  }

  public float getTotalTime() {
    return totalTime;
  }

  public float getDungeonTime() {
    return dungeonTime;
  }

  public String getCurrentDungeonId() {
    return currentDungeonId;
  }

  /** Returns all dungeon times, including the live elapsed time for the active dungeon. */
  public Map<String, Float> getDungeonTimes() {
    Map<String, Float> times = new LinkedHashMap<>(dungeonTimes);
    if (dungeonRunning && currentDungeonId != null) {
      times.put(currentDungeonId, dungeonTime);
    }
    return times;
  }

  public String formatTime(float seconds) {
    int totalSeconds = (int) seconds;
    int minutes = totalSeconds / 60;
    int secs = totalSeconds % 60;
    int hundredths = (int) ((seconds - totalSeconds) * 100);
    return String.format("%02d:%02d.%02d", minutes, secs, hundredths);
  }
}
