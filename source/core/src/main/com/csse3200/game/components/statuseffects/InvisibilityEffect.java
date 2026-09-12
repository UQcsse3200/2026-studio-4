package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

/**
 * The condition the Invisibility ability puts on the player. While it runs the player takes no
 * hostile damage and hostiles cannot target them.
 *
 * <p>It changes no raw combat stats of its own; the targeting and damage code reads it instead.
 */
public class InvisibilityEffect extends TimedStatusEffect {
  public InvisibilityEffect(GameTime time, long duration) {
    super(time, duration);
  }
}
