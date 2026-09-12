package com.csse3200.game.components.statuseffects;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.services.GameTime;

/**
 * The condition the Last Stand ability puts on the player. While it runs the wearer's effective
 * strength, movement speed and attack speed are amplified and it is drawn red.
 *
 * <p>It reads raw combat stats rather than changing them, so the burst cannot leak into the
 * wearer's real stats and adjustments made during it, such as picking up a charm, survive it.
 */
public class LastStandEffect extends TimedStatusEffect {
  /** Applied to effective strength, movement speed and attack speed while the effect runs. */
  public static final float MULTIPLIER = 1.5f;

  /** Red tint: the green and blue channels are cut, leaving red at full strength. */
  private static final Color TINT = new Color(1f, 0.35f, 0.35f, 1f);

  public LastStandEffect(GameTime time, long duration) {
    super(time, duration);
  }

  @Override
  public float getStatMultiplier() {
    return MULTIPLIER;
  }

  @Override
  public Color getTint() {
    return TINT;
  }
}
