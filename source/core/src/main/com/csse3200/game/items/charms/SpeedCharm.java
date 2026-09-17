package com.csse3200.game.items.charms;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;

public class SpeedCharm extends Charm {
  private static final float VALUE = 1f;
  public static final String TEXTURE = "images/speed_charm.png";

  public SpeedCharm() {
    super("Speed Charm", "You feel yourself getting faster.", TEXTURE);
  }

  @Override
  public void applyEffect(Entity player) {
    player.getComponent(CombatStatsComponent.class).addMovementSpeed(VALUE);
  }

  @Override
  public void removeEffect(Entity player) {
    player.getComponent(CombatStatsComponent.class).addMovementSpeed(-VALUE);
  }

  @Override
  public String getEffectSummary() {
    return "Speed +" + formatAmount(VALUE);
  }
}
