package com.csse3200.game.items.charms;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;

public class AttackSpeedCharm extends Charm {
  private static final float VALUE = 10f;
  public static final String TEXTURE = "images/ghost.png";

  public AttackSpeedCharm() {
    super("Strength Charm", "You feel yourself getting stronger.", TEXTURE);
  }

  @Override
  public void applyEffect(Entity player) {
    player.getComponent(CombatStatsComponent.class).addAttackSpeed(VALUE);
  }

  @Override
  public void removeEffect(Entity player) {
    player.getComponent(CombatStatsComponent.class).addAttackSpeed(-VALUE);
  }
}