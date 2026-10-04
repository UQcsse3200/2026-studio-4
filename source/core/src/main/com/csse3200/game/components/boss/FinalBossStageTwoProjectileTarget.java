package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.Vector2;

/** Target motion and collision response for one projectile update interval. */
record FinalBossStageTwoProjectileTarget(
    Vector2 previousPosition, Vector2 currentPosition, float radius, Runnable onHit) {

  boolean canBeHit() {
    return onHit != null
        && validPoint(previousPosition)
        && validPoint(currentPosition)
        && Float.isFinite(radius)
        && radius >= 0f;
  }

  private static boolean validPoint(Vector2 point) {
    return point != null && Float.isFinite(point.x) && Float.isFinite(point.y);
  }
}
