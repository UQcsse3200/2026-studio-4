package com.csse3200.game.components.achievements;

import java.util.function.Predicate;

/** A single achievement: a name plus a condition that decides when it unlocks. */
public class Achievement {
  private boolean unlocked = false;
  private final String name;
  private final Predicate<AchievementContext> condition;

  public Achievement(String name, Predicate<AchievementContext> condition) {
    this.name = name;
    this.condition = condition;
  }

  /** Checks this achievement against what just happened. Returns true if it just unlocked. */
  public boolean update(AchievementContext context) {
    if (unlocked) {
      return false;
    }
    if (condition.test(context)) {
      unlocked = true;
      return true;
    }
    return false;
  }

  public boolean isUnlocked() {
    return unlocked;
  }

  public String getName() {
    return name;
  }
}
