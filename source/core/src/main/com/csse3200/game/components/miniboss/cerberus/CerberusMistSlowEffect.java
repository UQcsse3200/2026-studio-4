package com.csse3200.game.components.miniboss.cerberus;

import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.components.statuseffects.StatusEffect;

public class CerberusMistSlowEffect implements StatusEffect {
  public static final float MOVEMENT_MULTIPLIER = 0.5f;

  @Override
  public boolean update() {
    return false;
  }

  @Override
  public long getRemainingDuration() {
    return Long.MAX_VALUE;
  }

  @Override
  public float getStatMultiplier(Stat stat) {
    return stat == Stat.MOVEMENT_SPEED ? MOVEMENT_MULTIPLIER : 1f;
  }
}
