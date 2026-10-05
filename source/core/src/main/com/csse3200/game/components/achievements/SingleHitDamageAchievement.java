package com.csse3200.game.components.achievements;

/** Unlocks the first time a single hit deals damage at or above a threshold. */
public class SingleHitDamageAchievement extends Achievement {
  private final int threshold;

  public SingleHitDamageAchievement(int threshold, String name) {
    super(name);
    this.threshold = threshold;
  }

  @Override
  public boolean onSingleHitDamage(int damage) {
    return damage >= threshold && unlock();
  }
}
