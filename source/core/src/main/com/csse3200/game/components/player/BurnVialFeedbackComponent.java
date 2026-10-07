package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * A short, unmissable orange flame icon drawn above the player's head whenever a Burn Vial is used,
 * so the effect is visible even though Burn Vial has no in-world sprite of its own yet.
 *
 * <p>Mirrors {@link HealingPotionFeedbackComponent}'s pattern: listen for {@link
 * ConsumableEffectComponent#USED}, then draw a simple tinted shape for a fixed duration.
 */
public class BurnVialFeedbackComponent extends RenderComponent {
  private static final float DURATION = 1.2f;
  private static final float TILE_SIZE = 0.22f;
  private Texture pixel;
  private float remaining;

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener(ConsumableEffectComponent.USED, this::onItemUsed);
  }

  private void onItemUsed(String id) {
    if (ItemIds.BURN_VIAL.equals(id)) {
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

    float alpha = remaining / DURATION;
    Vector2 centre = entity.getCenterPosition();
    float x = centre.x - TILE_SIZE / 2f;
    float originalColour = batch.getPackedColor();

    // Three stacked tiles, widest at the base, mimicking a simple flame silhouette.
    batch.setColor(0.85f, 0.15f, 0.05f, alpha);
    batch.draw(pixel, x - TILE_SIZE * 0.3f, 0.9f + centre.y, TILE_SIZE * 1.6f, TILE_SIZE);
    batch.setColor(1f, 0.5f, 0f, alpha);
    batch.draw(pixel, x - TILE_SIZE * 0.15f, 1.05f + centre.y, TILE_SIZE * 1.3f, TILE_SIZE);
    batch.setColor(1f, 0.85f, 0.2f, alpha);
    batch.draw(pixel, x, 1.2f + centre.y, TILE_SIZE, TILE_SIZE);
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
