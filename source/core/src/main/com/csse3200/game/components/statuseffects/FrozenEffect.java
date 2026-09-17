package com.csse3200.game.components.statuseffects;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.services.GameTime;

/**
 * Encases whoever it is on in ice: a lasting light blue tint, and neither a step nor a swing for
 * the duration.
 *
 * <p>Like every other declarative effect here it only describes itself. Movement and AI ask the
 * controller whether their entity is immobilised, and damage falls out of the zeroed attack stat,
 * so nothing in movement, AI or combat needs to know that a freeze spell exists.
 */
public class FrozenEffect extends TimedStatusEffect {
  /** Light blue: the red channel is cut hardest, leaving the sprite reading as ice. */
  private static final Color TINT = new Color(0.45f, 0.75f, 1f, 1f);

  /** The same ice laid on top, so near-black enemies read as frozen rather than unchanged. */
  private static final Color GLOW = new Color(0.35f, 0.65f, 0.95f, 0.5f);

  /**
   * @param time the shared gameplay clock
   * @param duration how long the target stays frozen, in milliseconds
   */
  public FrozenEffect(GameTime time, long duration) {
    super(time, duration);
  }

  /** Zeroes every stat, so contact damage from a frozen body lands for nothing as well. */
  @Override
  public float getStatMultiplier(Stat stat) {
    return 0f;
  }

  @Override
  public boolean immobilisesOwner() {
    return true;
  }

  @Override
  public Color getTint() {
    return TINT;
  }

  @Override
  public Color getGlow() {
    return GLOW;
  }
}
