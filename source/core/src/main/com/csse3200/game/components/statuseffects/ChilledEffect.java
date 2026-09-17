package com.csse3200.game.components.statuseffects;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.services.GameTime;

/** 解冻以后只减速，不锁住玩家的操作。 */
public class ChilledEffect extends TimedStatusEffect {
  private final float speedMultiplier;

  public ChilledEffect(GameTime time, long duration, float speedMultiplier) {
    super(time, duration);
    this.speedMultiplier = speedMultiplier;
  }

  @Override
  public float getStatMultiplier(Stat stat) {
    return stat == Stat.MOVEMENT_SPEED ? speedMultiplier : 1f;
  }

  @Override
  public Color getTint() {
    return new Color(0.65f, 0.85f, 1f, 1f);
  }
}
