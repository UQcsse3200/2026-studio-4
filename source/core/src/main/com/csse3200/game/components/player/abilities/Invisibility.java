package com.csse3200.game.components.player.abilities;

import com.csse3200.game.components.player.TimedPlayerAbility;
import com.csse3200.game.components.statuseffects.InvisibilityEffect;
import com.csse3200.game.services.GameTime;

/**
 * Cast ability. Activating it puts an InvisibilityEffect on the player, and for as long as that
 * effect runs the player takes no hostile damage and hostiles cannot target them.
 *
 * <p>The ability only decides when to apply the effect. Everything that reacts to being hidden asks
 * the status effects controller, which also runs the countdown, so nothing outside this package
 * names this class.
 */
public final class Invisibility extends TimedPlayerAbility {
  /** Name carried by the abilityUsed, abilityEnded and abilityFailed events. */
  public static final String NAME = "invisibility";

  public static final long DURATION_MS = 15_000;
  public static final long COOLDOWN_MS = 45_000;

  public Invisibility(GameTime time) {
    super(NAME, COOLDOWN_MS, true, new InvisibilityEffect(time, DURATION_MS));
  }

  @Override
  public boolean isCastable() {
    return true;
  }
}
