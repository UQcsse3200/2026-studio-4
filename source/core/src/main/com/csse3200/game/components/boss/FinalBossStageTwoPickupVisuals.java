package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;

/** Read-only encounter visuals backed entirely by room-owned artwork. */
final class FinalBossStageTwoPickupVisuals {
  private static final float BAR_WIDTH = 0.11f;
  private static final float BAR_GAP = 0.045f;
  private static final float BAR_HEIGHT = 0.72f;
  private static final float BAR_GROUP_WIDTH = BAR_WIDTH * 2f + BAR_GAP;
  private final TextureRegion[] gems = FinalBossStageTwoAssets.pickupFrames();
  private final TextureRegion[] sparks = FinalBossStageTwoAssets.pickupSparkFrames();
  private final TextureRegion[] aura = FinalBossStageTwoAssets.iceAuraFrames();
  private final TextureRegion[] disappear = FinalBossStageTwoAssets.icePickupDisappearFrames();
  // This source pixel is opaque pale blue (193,232,248), so blue tints keep fills readable.
  private final TextureRegion pixel =
      new TextureRegion(FinalBossStageTwoAssets.obstacleRegion(), 27, 30, 1, 1);

  void drawGround(SpriteBatch batch, FinalBossStageTwoPickupController pickups) {
    if (pickups == null) return;
    Rectangle arena = pickups.getArenaBounds();
    if (!validArena(arena)) return;
    float colour = batch.getPackedColor();
    try {
      for (FinalBossStageTwoPickupController.Pickup pickup : pickups.pickups) {
        float elapsed = (float) pickup.elapsed;
        float remaining = pickups.getPickupLifetime() - elapsed;
        if (remaining <= 0f) continue;
        float alpha = remaining < 1.5f ? 0.45f + 0.55f * Math.abs(MathUtils.sin(elapsed * 6f)) : 1f;
        batch.setColor(1f, 1f, 1f, alpha);
        float hover = 0.06f * MathUtils.sin(elapsed * 4f);
        Rectangle box =
            fit(arena, pickup.position.x - 0.18f, pickup.position.y - 0.30f + hover, 0.36f, 0.60f);
        draw(batch, FinalBossStageTwoAssets.frame(gems, elapsed, 1f, true), box);
      }
      float duration = pickups.getEffectDuration();
      for (FinalBossStageTwoPickupController.Burst burst : pickups.bursts) {
        if (duration <= 0f || burst.elapsed >= duration) continue;
        float progress = MathUtils.clamp(burst.elapsed / duration, 0f, 1f);
        batch.setColor(1f, 1f, 1f, 1f - MathUtils.clamp((progress - 0.6f) / 0.4f, 0f, 1f));
        Rectangle box =
            fit(arena, burst.position.x - 0.35f, burst.position.y - 0.3325f, 0.70f, 0.665f);
        draw(batch, FinalBossStageTwoAssets.frame(sparks, burst.elapsed, duration, false), box);
      }
      float disappearDuration = pickups.getDisappearDuration();
      for (FinalBossStageTwoPickupController.Burst burst : pickups.disappearances) {
        if (disappearDuration <= 0f || burst.elapsed >= disappearDuration) continue;
        float progress = MathUtils.clamp(burst.elapsed / disappearDuration, 0f, 1f);
        batch.setColor(1f, 1f, 1f, 1f - MathUtils.clamp((progress - 0.6f) / 0.4f, 0f, 1f));
        Rectangle box = fit(arena, burst.position.x - 0.45f, burst.position.y - 0.45f, 0.9f, 0.9f);
        draw(
            batch,
            FinalBossStageTwoAssets.frame(disappear, burst.elapsed, disappearDuration, false),
            box);
      }
    } finally {
      batch.setPackedColor(colour);
    }
  }

  void drawPlayerBuff(
      SpriteBatch batch, FinalBossStageTwoPickupController pickups, Entity player, float elapsed) {
    if (pickups == null || player == null || pickups.getChargeCount() == 0) return;
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats != null && stats.isDead()) return;
    Rectangle arena = pickups.getArenaBounds();
    if (!validArena(arena)) return;
    float colour = batch.getPackedColor();
    try {
      Vector2 position = player.getPosition();
      Vector2 size = player.getScale();
      batch.setColor(1f, 1f, 1f, 1f);
      Rectangle head =
          fit(arena, position.x + size.x / 2f - 0.30f, position.y + size.y + 0.06f, 0.60f, 0.80f);
      draw(batch, FinalBossStageTwoAssets.frame(aura, elapsed, 0.65f, true), head);

      float x = position.x + size.x + 0.08f;
      if (x + BAR_GROUP_WIDTH > arena.x + arena.width) {
        x = position.x - 0.08f - BAR_GROUP_WIDTH;
      }
      Rectangle bars =
          fit(arena, x, position.y + (size.y - BAR_HEIGHT) / 2f, BAR_GROUP_WIDTH, BAR_HEIGHT);
      float scale = bars.width / BAR_GROUP_WIDTH;
      for (int slot = 0; slot < 2; slot++) {
        drawBar(
            batch,
            bars.x + slot * (BAR_WIDTH + BAR_GAP) * scale,
            bars.y,
            BAR_WIDTH * scale,
            bars.height,
            0.016f * scale,
            MathUtils.clamp(pickups.getChargeFraction(slot), 0f, 1f));
      }
    } finally {
      batch.setPackedColor(colour);
    }
  }

  private void drawBar(
      SpriteBatch batch,
      float x,
      float y,
      float width,
      float height,
      float border,
      float fraction) {
    if (fraction <= 0f) return;
    batch.setColor(0.15f, 0.23f, 0.35f, 1f);
    batch.draw(pixel, x, y, width, height);
    batch.setColor(0.04f, 0.08f, 0.14f, 0.95f);
    batch.draw(pixel, x + border, y + border, width - border * 2f, height - border * 2f);
    batch.setColor(0.20f, 0.65f, 1f, 1f);
    batch.draw(
        pixel, x + border, y + border, width - border * 2f, (height - border * 2f) * fraction);
  }

  private static boolean validArena(Rectangle arena) {
    return arena != null
        && Float.isFinite(arena.x)
        && Float.isFinite(arena.y)
        && Float.isFinite(arena.width)
        && Float.isFinite(arena.height)
        && arena.width > 0f
        && arena.height > 0f;
  }

  private static Rectangle fit(Rectangle arena, float x, float y, float width, float height) {
    float scale = Math.min(1f, Math.min(arena.width / width, arena.height / height));
    float fittedWidth = width * scale;
    float fittedHeight = height * scale;
    return new Rectangle(
        MathUtils.clamp(x, arena.x, arena.x + arena.width - fittedWidth),
        MathUtils.clamp(y, arena.y, arena.y + arena.height - fittedHeight),
        fittedWidth,
        fittedHeight);
  }

  private static void draw(SpriteBatch batch, TextureRegion frame, Rectangle bounds) {
    batch.draw(frame, bounds.x, bounds.y, bounds.width, bounds.height);
  }
}
