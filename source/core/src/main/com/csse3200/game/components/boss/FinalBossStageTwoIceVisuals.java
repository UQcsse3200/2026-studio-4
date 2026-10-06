package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/** Draws ice cover using room-owned textures; this helper owns no disposable resources. */
final class FinalBossStageTwoIceVisuals {
  private final TextureRegion obstacle;
  private final TextureRegion crackPixel;
  private final TextureRegion[] shatter;
  private final TextureRegion[] spawn;

  FinalBossStageTwoIceVisuals() {
    obstacle = FinalBossStageTwoAssets.obstacleRegion();
    // An opaque pale pixel at source (155,94), retained inside the original shared texture.
    crackPixel = new TextureRegion(obstacle, 27, 30, 1, 1);
    shatter = FinalBossStageTwoAssets.shatterFrames();
    spawn = FinalBossStageTwoAssets.iceSpawnFrames();
  }

  void draw(SpriteBatch batch, FinalBossStageTwoIceController ice) {
    if (ice == null) return;
    float colour = batch.getPackedColor();
    try {
      for (FinalBossStageTwoIceController.Cover cover : ice.covers) {
        if (cover.hitsRemaining > 0) {
          drawSpawn(batch, cover, ice.getSpawnDuration());
          drawCover(batch, cover, ice.getMaxHits());
        }
      }
      for (FinalBossStageTwoIceController.Shatter burst : ice.shatters) {
        drawShatter(batch, burst, ice.getShatterDuration());
      }
    } finally {
      batch.setPackedColor(colour);
    }
  }

  /** Cover is already solid while the blue arrival effect plays once behind its visible body. */
  private void drawSpawn(
      SpriteBatch batch, FinalBossStageTwoIceController.Cover cover, float duration) {
    if (duration <= 0f || cover.spawnElapsed >= duration) return;
    float progress = MathUtils.clamp(cover.spawnElapsed / duration, 0f, 1f);
    float alpha = 1f - MathUtils.clamp((progress - 0.8f) / 0.2f, 0f, 1f);
    Rectangle bounds = cover.bounds;
    float size = Math.max(bounds.width, bounds.height) * 1.3f;
    batch.setColor(1f, 1f, 1f, alpha);
    batch.draw(
        FinalBossStageTwoAssets.frame(spawn, cover.spawnElapsed, duration, false),
        bounds.x + (bounds.width - size) / 2f,
        bounds.y + (bounds.height - size) / 2f,
        size,
        size);
  }

  private void drawCover(
      SpriteBatch batch, FinalBossStageTwoIceController.Cover cover, int maxHits) {
    Rectangle bounds = cover.bounds;
    float height = bounds.height;
    batch.setColor(1f, 1f, 1f, 1f);
    batch.draw(obstacle, bounds.x, bounds.y, bounds.width, height);
    if (cover.hitFlashRemaining > 0f) drawHitFlash(batch, cover, height);
    int damage = MathUtils.clamp(maxHits - cover.hitsRemaining, 0, 3);
    if (damage == 0) return;
    batch.setColor(0.25f, 0.45f, 0.6f, 1f);
    line(batch, bounds, 0.50f, 0.67f, 0.44f, 0.53f);
    line(batch, bounds, 0.44f, 0.53f, 0.57f, 0.40f);
    line(batch, bounds, 0.57f, 0.40f, 0.48f, 0.26f);
    if (damage >= 2) {
      line(batch, bounds, 0.44f, 0.53f, 0.31f, 0.47f);
      line(batch, bounds, 0.31f, 0.47f, 0.24f, 0.35f);
    }
    if (damage >= 3) {
      line(batch, bounds, 0.57f, 0.40f, 0.71f, 0.35f);
      line(batch, bounds, 0.71f, 0.35f, 0.76f, 0.23f);
    }
  }

  private void drawHitFlash(
      SpriteBatch batch, FinalBossStageTwoIceController.Cover cover, float height) {
    int source = batch.getBlendSrcFunc();
    int destination = batch.getBlendDstFunc();
    int sourceAlpha = batch.getBlendSrcFuncAlpha();
    int destinationAlpha = batch.getBlendDstFuncAlpha();
    try {
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
      float alpha = 0.9f * MathUtils.clamp(cover.hitFlashRemaining / 0.16f, 0f, 1f);
      batch.setColor(1f, 1f, 1f, alpha);
      batch.draw(obstacle, cover.bounds.x, cover.bounds.y, cover.bounds.width, height);
    } finally {
      batch.setBlendFunctionSeparate(source, destination, sourceAlpha, destinationAlpha);
    }
  }

  private void line(
      SpriteBatch batch, Rectangle bounds, float fromX, float fromY, float toX, float toY) {
    float x = bounds.x + fromX * bounds.width;
    float y = bounds.y + fromY * bounds.height;
    float dx = (toX - fromX) * bounds.width;
    float dy = (toY - fromY) * bounds.height;
    float length = (float) Math.sqrt(dx * dx + dy * dy);
    float thickness = bounds.width * 0.025f;
    batch.draw(
        crackPixel,
        x,
        y - thickness / 2f,
        0f,
        thickness / 2f,
        length,
        thickness,
        1f,
        1f,
        MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees);
  }

  private void drawShatter(
      SpriteBatch batch, FinalBossStageTwoIceController.Shatter burst, float duration) {
    if (duration <= 0f || burst.elapsed >= duration) return;
    float progress = MathUtils.clamp(burst.elapsed / duration, 0f, 1f);
    float alpha = 1f - MathUtils.clamp((progress - 0.65f) / 0.35f, 0f, 1f);
    batch.setColor(1f, 1f, 1f, alpha);
    float size = Math.max(burst.bounds.width * 2f, burst.bounds.height);
    batch.draw(
        FinalBossStageTwoAssets.frame(shatter, burst.elapsed, duration, false),
        burst.bounds.x + (burst.bounds.width - size) / 2f,
        burst.bounds.y,
        size,
        size);
  }
}
