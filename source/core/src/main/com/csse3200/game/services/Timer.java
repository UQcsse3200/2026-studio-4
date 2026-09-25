package com.csse3200.game.services;

public class Timer {
  private float TotalTime;
  private float dungeonTime;
  private String currentDungeonId;
  private boolean runRunning;
  private boolean dungeonRunning;

  public void startRun() {
    if (!runRunning) {
      runRunning = true;
      TotalTime = 0;
    }
  }

  public void startDungeon(String dungeonId) {
    if (!dungeonRunning) {
      dungeonRunning = true;
      currentDungeonId = dungeonId;
      dungeonTime = 0f;
    }
  }

  public void update(float delta) {
    if (runRunning) {
      TotalTime += delta;
    }
    if (dungeonRunning) {
      dungeonTime += delta;
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
    return TotalTime;
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
