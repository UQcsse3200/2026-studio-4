package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;

/** Player ice shots and the final-charge transformation; all textures remain room-owned. */
final class FinalBossStageTwoPlayerIceVisuals {
  private final TextureRegion[] shots = FinalBossStageTwoAssets.playerIceFrames();
  private final TextureRegion[] impacts = FinalBossStageTwoAssets.pickupSparkFrames();
  private final TextureRegion[] ending = FinalBossStageTwoAssets.iceBuffEndFrames();

  void drawShots(SpriteBatch batch, FinalBossStageTwoPlayerIceController ice) {
    if (ice == null) return;
    float colour = batch.getPackedColor();
    try {
      batch.setColor(1f, 1f, 1f, 1f);
      for (FinalBossStageTwoPlayerIceController.Shot shot : ice.shots) {
        float width = 1.05f;
        float height = 0.525f;
        // The source head is at (50,15) from the image's top left. Batch origins use the bottom.
        float anchorX = width * 50f / 64f;
        float anchorY = height * 17f / 32f;
        batch.draw(
            FinalBossStageTwoAssets.frame(shots, shot.elapsed, 0.3f, true),
            shot.position.x - anchorX,
            shot.position.y - anchorY,
            anchorX,
            anchorY,
            width,
            height,
            1f,
            1f,
            shot.velocity.angleDeg());
      }
      float duration = ice.getImpactDuration();
      for (FinalBossStageTwoPlayerIceController.Impact impact : ice.impacts) {
        if (duration <= 0f || impact.elapsed >= duration) continue;
        float progress = MathUtils.clamp(impact.elapsed / duration, 0f, 1f);
        float alpha = 1f - MathUtils.clamp((progress - 0.6f) / 0.4f, 0f, 1f);
        batch.setColor(0.55f, 0.85f, 1f, alpha);
        batch.draw(
            FinalBossStageTwoAssets.frame(impacts, impact.elapsed, duration, false),
            impact.position.x - 0.35f,
            impact.position.y - 0.3325f,
            0.7f,
            0.665f);
      }
    } finally {
      batch.setPackedColor(colour);
    }
  }

  void drawEnding(
      SpriteBatch batch, Entity player, Rectangle arena, float remaining, float duration) {
    if (player == null || remaining <= 0f || duration <= 0f) return;
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats != null && stats.isDead()) return;
    float colour = batch.getPackedColor();
    try {
      float elapsed = Math.max(0f, duration - remaining);
      float progress = MathUtils.clamp(elapsed / duration, 0f, 1f);
      float alpha = 1f - MathUtils.clamp((progress - 0.55f) / 0.45f, 0f, 1f);
      float size = 0.9f;
      boolean clamp = arena != null && arena.width > 0f && arena.height > 0f;
      if (clamp) size = Math.min(size, Math.min(arena.width, arena.height));
      Vector2 position = player.getPosition();
      Vector2 playerSize = player.getScale();
      float x = position.x + playerSize.x / 2f - size / 2f;
      float y = position.y + playerSize.y + 0.46f - size / 2f;
      if (clamp) {
        x = MathUtils.clamp(x, arena.x, arena.x + arena.width - size);
        y = MathUtils.clamp(y, arena.y, arena.y + arena.height - size);
      }
      batch.setColor(1f, 1f, 1f, alpha);
      batch.draw(FinalBossStageTwoAssets.frame(ending, elapsed, duration, false), x, y, size, size);
    } finally {
      batch.setPackedColor(colour);
    }
  }
}
