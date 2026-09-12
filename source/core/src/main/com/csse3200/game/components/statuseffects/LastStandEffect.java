package com.csse3200.game.components.statuseffects;

import com.csse3200.game.services.GameTime;

/**
 * The condition the Last Stand ability puts on the player. While it runs the player's effective
 * strength, movement speed and attack speed are amplified.
 *
 * <p>It reads raw combat stats rather than changing them, so the burst cannot leak into the
 * player's real stats.
 */
public class LastStandEffect extends TimedStatusEffect {
  public LastStandEffect(GameTime time, long duration) {
    super(time, duration);
  }
}
