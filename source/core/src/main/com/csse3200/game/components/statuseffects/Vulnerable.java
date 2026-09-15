package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.Damage;
import com.csse3200.game.services.GameTime;

public class Vulnerable implements StatusEffect, Damageable {
  private long duration;
  private long vulnerableInit;

  private final GameTime time = new GameTime();

  public Vulnerable(long duration) {
    this.duration = duration;
    vulnerableInit = time.getTime();
  }

  @Override
  public boolean damage(Damage damage) {
    int damageValue = damage.getDamage();
    damageValue = damageValue * 2;
    damage.setDamage(damageValue);
    return false;
  }

  @Override
  public boolean update() {
    if (time.getTimeSince(vulnerableInit) >= duration) {
      return true;
    }
    return false;
  }

  @Override
  public long getRemainingDuration() {
    return duration - time.getTimeSince(vulnerableInit);
  }
}
