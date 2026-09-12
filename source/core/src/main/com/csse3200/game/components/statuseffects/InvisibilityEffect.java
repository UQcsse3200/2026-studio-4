package com.csse3200.game.components.statuseffects;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.services.GameTime;

/**
 * The condition the Invisibility ability puts on the player. While it runs the entity wearing it
 * takes no hostile damage, hostiles cannot target it, and it is drawn faded.
 *
 * <p>It changes no raw combat stats of its own. Nothing here is specific to the player, so putting
 * one of these on an enemy hides that enemy in exactly the same way.
 */
public class InvisibilityEffect extends TimedStatusEffect {
  /**
   * Opacity the wearer is drawn at, low enough to read as hidden but still visible to the owner.
   */
  private static final Color TINT = new Color(1f, 1f, 1f, 0.35f);

  public InvisibilityEffect(GameTime time, long duration) {
    super(time, duration);
  }

  @Override
  public boolean concealsOwner() {
    return true;
  }

  @Override
  public Color getTint() {
    return TINT;
  }
}
