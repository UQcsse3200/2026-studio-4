package com.csse3200.game.items.charms;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemIds;

public class StrengthCharm extends Charm {
  private static final int VALUE = 10;
  public static final String TEXTURE = "images/strength_charm_pixel.png";

  public StrengthCharm() {
    super(ItemIds.STRENGTH_CHARM, "Strength Charm", "You feel yourself getting stronger.", TEXTURE);
  }

  @Override
  public void applyEffect(Entity player) {
    player.getComponent(CombatStatsComponent.class).addBaseAttack(VALUE);
  }

  @Override
  public void removeEffect(Entity player) {
    player.getComponent(CombatStatsComponent.class).addBaseAttack(-VALUE);
  }

  @Override
  public String getEffectSummary() {
    return "Attack +" + VALUE;
  }
}
