package com.csse3200.game.components.achievements;

import com.csse3200.game.entities.Entity;

/** A single instance of achievement. */
public class Achievement {
  private final int maxProgression;
  private int currentProgression;
  private final String progression;
  private final String name;
  private boolean unlocked = false;
  private Entity room;

  /**
   * Constructs the new achievement.
   *
   * @param room The room entity in which to start this achievement.
   * @param progression The event that should progress this achievement.
   * @param maxProgression The number of progressions required to complete this achievement. If this
   *     achievement is doing 1 thing, the maxProgression should be 1.
   * @param name The name to display of this achievement.
   */
  public Achievement(Entity room, String progression, int maxProgression, String name) {
    this.progression = progression;
    this.name = name;
    this.maxProgression = maxProgression;
    newRoom(room); // sets the room and adds the listener
  }

  /**
   * @return The name of the achievement
   */
  public String getName() {
    return name;
  }

  /**
   * Moves this achievement to a new room.
   *
   * @param room the room to move to.
   */
  public void newRoom(Entity room) {
    this.room = room;
    room.getEvents().addListener(progression, this::progress);
  }

  /** Progresses the achievement by 1. */
  private void progress() {
    if (unlocked) {
      return;
    }
    currentProgression++;
    if (currentProgression >= maxProgression) {
      unlocked = true;
      room.getEvents().trigger("achievementUnlocked", name);
    }
  }

  /**
   * @return the maximum number of progressions for this room to work.
   */
  public int getMaxProgression() {
    return maxProgression;
  }

  /**
   * @return the current progressions for this room.
   */
  public int getCurrentProgression() {
    return currentProgression;
  }

  public boolean isUnlocked() {
    return unlocked;
  }
}
