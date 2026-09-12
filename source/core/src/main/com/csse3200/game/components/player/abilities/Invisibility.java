package com.csse3200.game.components.player.abilities;

import com.csse3200.game.components.player.TimedPlayerAbility;
import com.csse3200.game.components.statuseffects.InvisibilityEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;

/**
 * Cast ability. Activating it puts an InvisibilityEffect on the player, and for as long as that
 * effect runs the player takes no hostile damage and hostiles cannot target them.
 *
 * <p>The ability owns the effect. It decides when to apply it and reports the player's state from
 * it, while the status effects controller runs the countdown.
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

  /** Safe detection hook: null targets and targets without abilities are visible. */
  public static boolean isActiveOn(Entity target) {
    return isRunningOn(target, Invisibility.class);
  }

  /**
   * Returns whether hostiles should ignore the target, either because there is no target or because
   * invisibility is active. Callers that pass this check may dereference the target.
   */
  public static boolean isUntargetable(Entity target) {
    return target == null || isActiveOn(target);
  }
}
