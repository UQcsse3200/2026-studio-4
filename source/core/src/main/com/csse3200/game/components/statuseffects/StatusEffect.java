package com.csse3200.game.components.statuseffects;

/** Defines the behavior of status effects. Status effects implement these methods. */
public interface StatusEffect {
  /**
   * Updates the status effect.
   *
   * @return true if the status effect is to be removed. false otherwise.
   */
  boolean update();

  /**
   * @return the time remaining until the status effect should be removed in milliseconds.
   */
  long getRemainingDuration();
}
