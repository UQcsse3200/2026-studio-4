package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.items.consumables.InstantHealingPotion;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** A short rising red cross when an instant healing potion succeeds. */
public class HealingPotionFeedbackComponent extends RenderComponent {
  private static final float DURATION = 0.6f;
  private static final float BAR_LENGTH = 0.3f;
  private static final float BAR_WIDTH = 0.07f;
  private Texture pixel;
  private float remaining;

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener(ConsumableEffectComponent.USED, this::onItemUsed);
  }

  private void onItemUsed(String id) {
    if (InstantHealingPotion.isHealingPotionId(id)) {
      remaining = DURATION;
    }
  }

  @Override
  public void update() {
    if (remaining <= 0f) return;
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (Float.isFinite(delta) && delta > 0f) {
      remaining = Math.max(0f, remaining - delta);
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (remaining <= 0f || isRepeatPass()) return;
    if (pixel == null) {
      Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
      pixmap.setColor(Color.WHITE);
      pixmap.fill();
      pixel = new Texture(pixmap);
      pixmap.dispose();
    }

    float progress = 1f - remaining / DURATION;
    Vector2 centre = entity.getCenterPosition();
    float x = centre.x + 0.25f;
    float y = centre.y + 0.2f + progress * 0.35f;
    float originalColour = batch.getPackedColor();
    batch.setColor(1f, 0.15f, 0.15f, remaining / DURATION);
    batch.draw(pixel, x, y + (BAR_LENGTH - BAR_WIDTH) / 2f, BAR_LENGTH, BAR_WIDTH);
    batch.draw(pixel, x + (BAR_LENGTH - BAR_WIDTH) / 2f, y, BAR_WIDTH, BAR_LENGTH);
    batch.setPackedColor(originalColour);
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() + 0.1f;
  }

  @Override
  public void dispose() {
    if (pixel != null) pixel.dispose();
    super.dispose();
  }
}
