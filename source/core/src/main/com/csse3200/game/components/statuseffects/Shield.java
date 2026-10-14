package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.Damage;
import com.csse3200.game.services.GameTime;

public class Shield extends TimedStatusEffect implements StatusEffect, Damageable {

  private final GameTime time = new GameTime();

  public Shield(GameTime time, long duration) {
    super(time, duration);
  }

  @Override
  public boolean damage(Damage damage) {
    int damageValue = damage.getDamage();
    damageValue = 0;
    damage.setDamage(damageValue);
    return false;
  }
}
