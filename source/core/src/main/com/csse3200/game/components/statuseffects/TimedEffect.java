package com.csse3200.game.components.statuseffects;

/**
 * A status effect that runs for a while and then stops, which the status effects controller expires
 * on the owner's behalf.
 *
 * <p>This is only the contract the controller drives. It says nothing about what the effect does or
 * who applied it, so an effect the player puts on an enemy implements this exactly like one the
 * player puts on themselves.
 */
public interface TimedEffect extends StatusEffect {
  /** Returns whether the effect has started and not yet stopped. */
  boolean isActive();

  /**
   * Stops the effect without running its end callback, so the controller can clear a whole batch
   * before any callback runs and sees a half cleared batch.
   */
  void clear();

  /** Runs the end callback, after the controller has cleared the batch this effect belongs to. */
  void notifyEnded();
}
