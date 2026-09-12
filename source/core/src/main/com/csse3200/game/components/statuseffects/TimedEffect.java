package com.csse3200.game.components.statuseffects;

import com.badlogic.gdx.graphics.Color;

/**
 * A status effect that runs for a while and then stops, which the status effects controller expires
 * on the owner's behalf.
 *
 * <p>This is only the contract the controller drives. It says nothing about what the effect does or
 * who applied it, so an effect the player puts on an enemy implements this exactly like one the
 * player puts on themselves.
 *
 * <p>An effect also describes what it does to the entity it is on in general terms: whether it
 * hides them, how it scales their combat stats, how it tints them. Combat, rendering and enemy AI
 * ask {@link com.csse3200.game.components.StatusEffectsControllerComponent} for the combined answer
 * rather than asking any particular ability whether it is running, so a second source of, say,
 * concealment costs one new effect and no change to the code that reacts to it.
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

  /**
   * Returns whether hostiles should be unable to find, chase or damage the entity while this runs.
   */
  default boolean concealsOwner() {
    return false;
  }

  /**
   * Returns what this effect multiplies the entity's effective combat stats by while it runs. The
   * raw stats are never touched, so charms and other adjustments keep working underneath.
   */
  default float getStatMultiplier() {
    return 1f;
  }

  /**
   * Returns the colour this effect multiplies into the sprite of whatever it is on, or null to
   * leave the sprite alone. Lets an effect show itself without the renderer naming it.
   *
   * <p>Treat the returned colour as read only; the caller may be a renderer running every frame.
   */
  default Color getTint() {
    return null;
  }
}
