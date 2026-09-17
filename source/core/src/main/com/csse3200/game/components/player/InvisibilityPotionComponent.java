package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks invisibility duration and potion cooldown for the HUD.
 *
 * <p>Gameplay systems (AI, damage immunity) are notified through existing player events / combat
 * flags. Potion inventory consumption is left to the special-abilities item path; this component
 * only owns the 15s / 45s timers the HUD displays.
 */
public class InvisibilityPotionComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(InvisibilityPotionComponent.class);

  /** Time the player stays undetectable and damage-immune after a successful apply. */
  public static final long DURATION_MS = 15_000L;

  /** Cooldown measured from a successful production cast. */
  public static final long COOLDOWN_MS = 45_000L;

  private CombatStatsComponent combatStats;
  private boolean invisible;
  private long applyTimeMs;
  private long lastProductionCastMs = -1L;

  @Override
  public void create() {
    combatStats = entity.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("useInvisibilityPotion", this::tryUse);
  }

  /**
   * Production use: rejected while cooling down unless the player is still invisible (refresh).
   *
   * @return true if invisibility was applied or refreshed
   */
  public boolean tryUse() {
    if (!invisible && isProductionCooldownActive()) {
      logger.debug("Invisibility potion rejected: still on cooldown");
      return false;
    }
    applyInvisibility(true);
    return true;
  }

  /**
   * F1 {@code ability invisibility}: applies the real 15s effect and bypasses cooldown.
   *
   * @return true if the effect was applied
   */
  public boolean applyForQa() {
    applyInvisibility(false);
    return true;
  }

  @Override
  public void update() {
    if (!invisible) {
      return;
    }
    if (timeSince(applyTimeMs) >= DURATION_MS) {
      endInvisibility();
    }
  }

  /** Whether the player is currently stealthed. */
  public boolean isInvisible() {
    return invisible && timeSince(applyTimeMs) < DURATION_MS;
  }

  /**
   * Remaining stealth time for the HUD.
   *
   * @return milliseconds left, or 0 if not invisible
   */
  public long getRemainingDurationMs() {
    if (!isInvisible()) {
      return 0L;
    }
    return Math.max(0L, DURATION_MS - timeSince(applyTimeMs));
  }

  /**
   * Remaining production cooldown for the HUD.
   *
   * @return milliseconds left, or 0 if ready
   */
  public long getRemainingCooldownMs() {
    if (!isProductionCooldownActive()) {
      return 0L;
    }
    return Math.max(0L, COOLDOWN_MS - timeSince(lastProductionCastMs));
  }

  /** HUD line for remaining stealth. */
  public String getDurationHudText() {
    long remaining = getRemainingDurationMs();
    if (remaining > 0L) {
      return String.format("Invisibility: %ds", toDisplaySeconds(remaining));
    }
    return "Invisibility: Ready";
  }

  /** HUD line for remaining cooldown. */
  public String getCooldownHudText() {
    long remaining = getRemainingCooldownMs();
    if (remaining > 0L) {
      return String.format("Invis CD: %ds", toDisplaySeconds(remaining));
    }
    return "Invis CD: Ready";
  }

  private void applyInvisibility(boolean startsProductionCooldown) {
    invisible = true;
    applyTimeMs = now();
    if (startsProductionCooldown) {
      lastProductionCastMs = applyTimeMs;
    }
    if (combatStats != null) {
      combatStats.setInvulnerable(true);
    }
    entity.getEvents().trigger("invisiblePlayer");
    entity.getEvents().trigger("invisibilityStarted");
  }

  private void endInvisibility() {
    invisible = false;
    if (combatStats != null) {
      combatStats.setInvulnerable(false);
    }
    entity.getEvents().trigger("invisibilityEnded");
  }

  private boolean isProductionCooldownActive() {
    return lastProductionCastMs >= 0L && timeSince(lastProductionCastMs) < COOLDOWN_MS;
  }

  private static int toDisplaySeconds(long remainingMs) {
    return (int) Math.max(1L, (remainingMs + 999L) / 1000L);
  }

  private long now() {
    GameTime time = ServiceLocator.getTimeSource();
    return time == null ? 0L : time.getTime();
  }

  private long timeSince(long thenMs) {
    return now() - thenMs;
  }
}
