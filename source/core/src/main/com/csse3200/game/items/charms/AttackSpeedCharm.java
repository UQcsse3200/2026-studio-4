package com.csse3200.game.items.charms;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemType;

public class AttackSpeedCharm extends Charm {
  private static final float VALUE = 1f;
  public static final String TEXTURE = "images/attack_speed_charm.png";

  public AttackSpeedCharm() {
    super(ItemType.ATTACK_SPEED_CHARM);
  }

  @Override
  public ItemType getItemType() {
    return ItemType.ATTACK_SPEED_CHARM;
  }

  @Override
  public void applyEffect(Entity player) {
    player.getComponent(CombatStatsComponent.class).addAttackSpeed(VALUE);
  }

  @Override
  public void removeEffect(Entity player) {
    player.getComponent(CombatStatsComponent.class).addAttackSpeed(-VALUE);
  }

  @Override
  public String getEffectSummary() {
    return "Attack Speed +" + formatAmount(VALUE);
  }
}
