package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.Vector2;

/** Shared visual dimensions keep fireball tails clear of the boss while close to its body. */
final class FinalBossStageTwoFireGeometry {
  static final float SHIELD_SCALE = 1.25f;
  static final float FIREBALL_SIZE = 1.15f;
  static final float FIREBALL_HEAD_X = 50f / 64f;

  private FinalBossStageTwoFireGeometry() {}

  static float spawnRadius(Vector2 bossSize) {
    // The wizard is drawn at 80% of the entity size; half the entity size leaves body clearance.
    // Tails may overlap the decorative shield so emission stays visually connected to the boss.
    return Math.max(bossSize.x, bossSize.y) / 2f + FIREBALL_SIZE * FIREBALL_HEAD_X + 0.08f;
  }
}
