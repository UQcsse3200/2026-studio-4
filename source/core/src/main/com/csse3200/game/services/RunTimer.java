package com.csse3200.game.services;

public class RunTimer {
  private final GameTime gameTime;
  private float totalTime;
  private float dungeonTime;
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
    }
  }

  public void startDungeon(String dungeonId) {
    if (!dungeonRunning) {
      dungeonRunning = true;
      currentDungeonId = dungeonId;
      dungeonTime = 0f;
    }
  }

  /** Call once per frame. Uses scaled delta so pausing via timeScale pauses the timers. */
  public void update() {
    float delta = gameTime.getDeltaTime();
    if (runRunning) totalTime += delta;
    if (dungeonRunning) dungeonTime += delta;
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

  public String formatTime(float seconds) {
    int totalSeconds = (int) seconds;
    int minutes = totalSeconds / 60;
    int secs = totalSeconds % 60;
    int hundredths = (int) ((seconds - totalSeconds) * 100);
    return String.format("%02d:%02d.%02d", minutes, secs, hundredths);
  }
}
