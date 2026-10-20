package com.csse3200.game.components.statuseffects;

import com.csse3200.game.components.Damage;
import com.csse3200.game.services.GameTime;

public class Vulnerable extends StatusEffect implements Damageable {
  private final long duration;
  private final long vulnerableInit;


  public Vulnerable(long duration) {
      super(new GameTime(), duration);
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
    return time.getTimeSince(vulnerableInit) >= duration;
  }

  @Override
  public long getRemainingDuration() {
    return duration - time.getTimeSince(vulnerableInit);
  }
}
