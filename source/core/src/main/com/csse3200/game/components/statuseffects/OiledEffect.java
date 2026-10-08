package com.csse3200.game.components.statuseffects;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.services.GameTime;

/** Marks an entity as covered in oil for a short time. */
public class OiledEffect extends TimedStatusEffect {
  private static final Color OIL_TINT = new Color(0.72f, 0.58f, 0.28f, 1f);

  public OiledEffect(GameTime time, long duration) {
    super(time, duration);
  }

  @Override
  public boolean isOiled() {
    return true;
  }

  @Override
  public Color getTint() {
    return OIL_TINT;
  }
}
