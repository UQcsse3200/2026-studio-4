package com.csse3200.game.components.tasks;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * A self-contained damage-over-time effect (poison). Not part of the Entity/Component system - it's
 * owned and driven by whichever attack applies it, since components can't be added to an
 * already-created entity at runtime.
 */
public class PoisonEffect {
  private final int damagePerTick;
  private final long tickIntervalMs;
  private final long totalDurationMs;
  private final CombatStatsComponent target;
  private final GameTime gameTime;

  private final long startTime;
  private long lastTickTime;

  public PoisonEffect(
      CombatStatsComponent target, int damagePerTick, long tickIntervalMs, long totalDurationMs) {
    this.target = target;
    this.damagePerTick = damagePerTick;
    this.tickIntervalMs = tickIntervalMs;
    this.totalDurationMs = totalDurationMs;
    this.gameTime = ServiceLocator.getTimeSource();
    this.startTime = gameTime.getTime();
    this.lastTickTime = startTime;
  }

  /**
   * Call this once per frame while the effect is active.
   *
   * @return true once the effect has finished and should be discarded.
   */
  public boolean update() {
    long now = gameTime.getTime();
    if (now - lastTickTime >= tickIntervalMs) {
      target.takeDamage(damagePerTick);
      lastTickTime = now;
    }
    return now - startTime >= totalDurationMs;
  }
}
