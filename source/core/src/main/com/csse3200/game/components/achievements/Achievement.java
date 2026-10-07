package com.csse3200.game.components.achievements;

import java.util.function.Predicate;

/**
 * A single achievement: a name, a condition checked on update, and a shared progress value some
 * conditions use to track a running count or measurement between updates.
 */
public class Achievement {
  private boolean unlocked = false;
  private final String name;
  private Predicate<AchievementContext> condition;
  private float progress;
  private float target; // 0 means no numeric target to show

  /** Creates an achievement with its condition fixed at construction. */
  public Achievement(String name, Predicate<AchievementContext> condition) {
    this.name = name;
    this.condition = condition;
  }

  /** Creates an achievement whose condition is attached afterward via {@link #setCondition}. */
  public Achievement(String name) {
    this.name = name;
  }

  /**
   * Sets this achievement's condition. Used by conditions that need to close over {@code this} (to
   * call {@link #addProgress} or {@link #setProgress}), which isn't possible while still inside the
   * constructor call that would otherwise take the condition as an argument.
   */
  public void setCondition(Predicate<AchievementContext> condition) {
    this.condition = condition;
  }

  /** Checks this achievement's condition against what just happened. */
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

  public float getProgress() {
    return progress;
  }

  public void setProgress(float progress) {
    this.progress = progress;
  }

  public void addProgress(float amount) {
    this.progress += amount;
  }

  public float getTarget() {
    return target;
  }

  public void setTarget(float target) {
    this.target = target;
  }

  public boolean isUnlocked() {
    return unlocked;
  }

  public String getName() {
    return name;
  }

  public void restoreState(boolean unlocked, float progress) {
    this.unlocked = unlocked;
    this.progress = progress;
  }
}
