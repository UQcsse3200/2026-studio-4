package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.items.ItemIds;

/** Blue arc inside the speed potion slot, showing the actual remaining effect time. */
class SpeedPotionTimerRing extends Actor {
  private static final int SEGMENTS = 48;
  private static final float RADIUS = 29f;
  private static final float THICKNESS = 3f;
  private static final float STEP = MathUtils.PI2 / SEGMENTS;
  private final ConsumableEffectComponent effects;
  private final TextureRegion pixel;

  SpeedPotionTimerRing(ConsumableEffectComponent effects, Texture pixel) {
    this.effects = effects;
    this.pixel = new TextureRegion(pixel);
    setName("speed-potion-timer-ring");
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    int visible = (int) Math.ceil(SEGMENTS * effects.getRemainingFraction(ItemIds.SPEED_POTION));
    if (visible == 0) return;

    float originalColour = batch.getPackedColor();
    batch.setColor(0.25f, 0.72f, 1f, parentAlpha);
    float centreX = getX() + getWidth() / 2f;
    float centreY = getY() + getHeight() / 2f;
    float scale = Math.min(getWidth(), getHeight()) / 72f;
    float radius = RADIUS * scale;
    float thickness = THICKNESS * scale;
    for (int i = 0; i < visible; i++) {
      float start = MathUtils.PI / 2f - i * STEP;
      float end = start - STEP;
      float x = centreX + radius * MathUtils.cos(start);
      float y = centreY + radius * MathUtils.sin(start);
      float nextX = centreX + radius * MathUtils.cos(end);
      float nextY = centreY + radius * MathUtils.sin(end);
      float length = (float) Math.hypot(nextX - x, nextY - y) + 0.5f * scale;
      float angle = MathUtils.atan2(nextY - y, nextX - x) * MathUtils.radiansToDegrees;
      batch.draw(
          pixel, x, y - thickness / 2f, 0f, thickness / 2f, length, thickness, 1f, 1f, angle);
    }
    batch.setPackedColor(originalColour);
  }
}
