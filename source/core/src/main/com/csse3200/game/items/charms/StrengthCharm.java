package com.csse3200.game.items.charms;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemType;

public class StrengthCharm extends Charm {
  private static final int VALUE = 10;
  public static final String TEXTURE = "images/strength_charm_pixel.png";

  public StrengthCharm() {
    super(ItemType.STRENGTH_CHARM);
  }

  @Override
  public ItemType getItemType() {
    return ItemType.STRENGTH_CHARM;
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
