package com.csse3200.game.services;

public class RunTimer {
  private final GameTime gameTime;
  private float totalTime;
  private float dungeonTime;
  private String currentDungeonId;
  private boolean runRunning;
  private boolean dungeonRunning;
  private float dungeonSyncedTime; // whole seconds, flips with the run clock
  private float dungeonStartTotal; // totalTime when the dungeon began
  private static final float MAX_DELTA =
      0.25f; // ignore anything beyond a one quarter-second hiccup

  public RunTimer(GameTime gameTime) {
    this.gameTime = gameTime;
  }

  public void startRun() {
    if (!runRunning) {
      runRunning = true;
      totalTime = 0;
    }
  }

  public void startDungeon(String dungeonId) {
    if (!dungeonRunning) {
      dungeonRunning = true;
      currentDungeonId = dungeonId;
      dungeonStartTotal = totalTime;
      dungeonTime = 0f;
      dungeonSyncedTime = 0f;
    }
  }

  /** Call once per frame. Uses scaled delta so pausing via timeScale pauses the timers. */
  public void update() {
    float delta = Math.min(gameTime.getDeltaTime(), MAX_DELTA);
    if (runRunning) totalTime += delta;
    if (dungeonRunning) {
      dungeonTime = totalTime - dungeonStartTotal;
      // Whole seconds on the run clock's boundaries, so both labels tick together
      dungeonSyncedTime = ((float) (int) totalTime) - (int) dungeonStartTotal;
    }
  }

  public void stopDungeon() {
    dungeonRunning = false;
    currentDungeonId = null;
  }

  public void stopRun() {
    runRunning = false;
    dungeonRunning = false;
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
}
