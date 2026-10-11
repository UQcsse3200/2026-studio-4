package com.csse3200.game.services;

import java.util.*;

public class RunTimer {
  private final GameTime gameTime;
  private float totalTime;
  private float dungeonTime;
  private final Map<String, Float> dungeonTimes = new LinkedHashMap<>();
  private String currentDungeonId;
  private boolean runRunning;
  private boolean dungeonRunning;
  private float dungeonSyncedTime; // whole seconds, flips with the run clock
  private float dungeonStartTotal; // totalTime when the dungeon began
  private boolean paused = false;
  private static final float MAX_DELTA =
      0.25f; // ignore anything beyond a one quarter-second hiccup

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

  public Map<String, Float> getDungeonTimes() {
    Map<String, Float> times = new LinkedHashMap<>(dungeonTimes);

    // Include the dungeon currently being timed when creating a save.
    if (dungeonRunning && currentDungeonId != null) {
      times.put(currentDungeonId, dungeonTime);
    }

    return times;
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
    if (dungeonRunning && Objects.equals(currentDungeonId, dungeonId)) {
      return;
    }

    if (dungeonRunning) {
      stopDungeon();
    }

    registerDungeon(dungeonId);

    // Continue from the previously saved time for this dungeon.
    float savedTime = dungeonTimes.getOrDefault(dungeonId, 0f);
    currentDungeonId = dungeonId;
    dungeonTime = savedTime;
    dungeonStartTotal = totalTime - savedTime;
    dungeonSyncedTime = ((float) (int) totalTime) - (int) dungeonStartTotal;
    dungeonRunning = true;
  }

  /** Call once per frame. Uses scaled delta so pausing via timeScale pauses the timers. */
  public void update() {
    if (isPaused()) {
      return;
    }
    float delta = Math.min(gameTime.getDeltaTime(), MAX_DELTA);
    if (runRunning) totalTime += delta;
    if (dungeonRunning) {
      dungeonTime = totalTime - dungeonStartTotal;
      // Whole seconds on the run clock's boundaries, so both labels tick together
      dungeonSyncedTime = ((float) (int) totalTime) - (int) dungeonStartTotal;
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

  /** Dungeon time in whole seconds, aligned to the run clock's second boundaries. */
  public float getDungeonSyncedTime() {
    return dungeonSyncedTime;
  }

  public String formatTime(float seconds) {
    int totalSeconds = (int) seconds;
    int minutes = totalSeconds / 60;
    int secs = totalSeconds % 60;
    int hundredths = (int) ((seconds - totalSeconds) * 100);
    return String.format("%02d:%02d.%02d", minutes, secs, hundredths);
  }

  public String formatTimeWithNoMilliSec(float seconds) {
    int totalSeconds = (int) seconds;
    int minutes = totalSeconds / 60;
    int secs = totalSeconds % 60;
    return String.format("%02d:%02d", minutes, secs);
  }

  /**
   * Requests that time stop advancing. Each owner's request is independent; call requestResume with
   * the same owner to release it.
   */
  public void requestPause() {
    paused = true;
  }

  public void requestResume() {
    paused = false;
  }

  public boolean isPaused() {
    return paused;
  }
}
