package com.csse3200.game.items.consumables;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.statuseffects.Burning;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Item-specific appearance around the existing Burning tick; damage and cadence stay in Burning.
 */
public final class VialBurning extends Burning {
  private static final float PULSE_SECONDS = 0.18f;
  private final CombatStatsComponent stats;
  private final Color glow = new Color(1f, 0.32f, 0.04f, 0f);
  private float pulseRemaining;

  public VialBurning(int damage, long cooldown, long duration, CombatStatsComponent stats) {
    super(damage, cooldown, duration, stats);
    this.stats = stats;
  }

  @Override
  public boolean update() {
    GameTime time = ServiceLocator.getTimeSource();
    float delta = time == null ? 0f : time.getDeltaTime();
    if (Float.isFinite(delta) && delta > 0f) {
      pulseRemaining = Math.max(0f, pulseRemaining - delta);
    }
    int healthBefore = stats.getHealth();
    boolean ended = super.update();
    // Invulnerability/shields may prevent HP loss. Other attacks never enter this comparison.
    if (stats.getHealth() < healthBefore) {
      pulseRemaining = PULSE_SECONDS;
    }
    return ended;
  }

  /** Strength of the brief surge following an actual burn health loss. */
  public float getPulseStrength() {
    return pulseRemaining / PULSE_SECONDS;
  }

  @Override
  public Color getGlow() {
    if (pulseRemaining <= 0f) return null;
    glow.a = 0.55f * getPulseStrength();
    return glow;
  }

  @Override
  public void onRemoved() {
    pulseRemaining = 0f;
  }
}
