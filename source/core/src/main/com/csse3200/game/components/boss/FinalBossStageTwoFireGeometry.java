package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.Vector2;

/** Shared visual dimensions keep the entire fireball tail outside the boss's fire shield. */
final class FinalBossStageTwoFireGeometry {
  static final float SHIELD_SCALE = 1.25f;
  static final float FIREBALL_SIZE = 1.15f;
  static final float FIREBALL_HEAD_X = 50f / 64f;

  private FinalBossStageTwoFireGeometry() {}

  static float spawnRadius(Vector2 bossSize) {
    return Math.max(bossSize.x, bossSize.y) * SHIELD_SCALE / 2f
        + FIREBALL_SIZE * FIREBALL_HEAD_X
        + 0.12f;
  }
}
