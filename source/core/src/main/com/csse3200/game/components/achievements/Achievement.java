package com.csse3200.game.components.achievements;

import com.csse3200.game.entities.Entity;

public class Achievement {
  private final int maxProgression;
  private int currentProgression;
  private String progression;
  private String name;

  public Achievement(Entity room, String progression, int maxProgression, String name) {
    this.progression = progression;
    this.name = name;
    this.maxProgression = maxProgression;
    currentProgression = 0;
    room.getEvents().addListener(progression, this::progress);
  }

  public String getName() {
    return name;
  }

  public void newRoom(Entity room) {
    room.getEvents().addListener(progression, this::progress);
  }

  private void progress() {
    currentProgression += 1;
  }

  public int getMaxProgression() {
    return maxProgression;
  }

  public int getCurrentProgression() {
    return currentProgression;
  }
}
