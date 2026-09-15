package com.csse3200.game.components.statuseffects;

import com.badlogic.gdx.graphics.Color;

/**
 * Defines the behavior of status effects. Status effects implement these methods.
 *
 * <p>Every status effect lasts for some time and is driven by the status effects controller of the
 * entity it is on, which polls {@link #update()} once a frame and drops the effect when it reports
 * done. The controller only ever sees this interface, so what an effect does lives on the effect.
 *
 * <p>An effect does its work in one of two ways. Self-driven effects such as burning act on the
 * entity inside {@link #update()}. Declarative effects such as invisibility instead describe what
 * they do through the query methods below, and combat, rendering and enemy AI ask the controller
 * for the combined answer. The defaults are neutral, so an effect overrides only what it changes.
 */
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

  /** Returns whether the effect has run its course. Queries skip expired effects immediately. */
  default boolean isExpired() {
    return getRemainingDuration() <= 0;
  }

  /**
   * Returns whether hostiles should be unable to find, chase or damage the entity while this runs.
   */
  default boolean concealsOwner() {
    return false;
  }

  /**
   * Returns what this effect multiplies the given effective stat by while it runs. The raw stats
   * are never touched, so charms and other adjustments keep working underneath and removing the
   * effect cannot leave a rounding remainder behind.
   */
  default float getStatMultiplier(Stat stat) {
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

  /**
   * Called once by the controller after the effect has been taken off its entity, whether it
   * expired, was removed early, or the entity died or was disposed. Runs after the whole batch of
   * expiring effects has been removed, so nothing observing from here sees a half-cleared list.
   */
  default void onRemoved() {}
}
