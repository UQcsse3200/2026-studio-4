package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.Vector2;

/** Shared visual dimensions keep fireball heads close to the boss's fire circle. */
final class FinalBossStageTwoFireGeometry {
  static final float SHIELD_SCALE = 1.25f;
  static final float FIREBALL_SIZE = 1.15f;
  static final float FIREBALL_HEAD_X = 50f / 64f;

  private FinalBossStageTwoFireGeometry() {}

  static float spawnRadius(Vector2 bossSize) {
    // Keep the head just outside the circle; the tail may overlap the ring and wizard.
    return Math.max(bossSize.x, bossSize.y) * SHIELD_SCALE / 2f + 0.3f;
  }
}
