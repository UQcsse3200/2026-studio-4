package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Consumable-only shield appearance; protection and expiry remain owned by the effect component.
 */
public class ShieldPotionFeedbackComponent extends RenderComponent {
  private ConsumableEffectComponent effects;
  private CombatStatsComponent stats;
  private GameTime time;
  private Texture shieldTexture;
  private long started;
  private boolean disposed;

  @Override
  public void create() {
    effects = entity.getComponent(ConsumableEffectComponent.class);
    stats = entity.getComponent(CombatStatsComponent.class);
    time = ServiceLocator.getTimeSource();
    entity
        .getEvents()
        .addListener(
            ConsumableEffectComponent.USED,
            (String id) -> {
              if (!disposed
                  && ItemIds.SHIELD.equals(id)
                  && time != null
                  && effects != null
                  && effects.isShielded()) {
                started = time.getTime();
              }
            });
    super.create();
  }

  // Like the existing Snake shield, this barrier keeps its colour without a duplicate glow pass.
  @Override
  public void render(SpriteBatch batch) {
    draw(batch);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (disposed
        || stats == null
        || stats.isDead()
        || effects == null
        || time == null
        || !effects.isShielded()) return;
    if (shieldTexture == null) shieldTexture = createShieldTexture();
    long elapsed = Math.max(0, time.getTime() - started);
    float opening = Math.min(1f, elapsed / 220f);
    float expansion = 1f - (1f - opening) * (1f - opening);
    Vector2 centre = entity.getCenterPosition();
    float size = Math.max(entity.getScale().x, entity.getScale().y);
    float diameter = size * (0.7f + expansion * 0.75f);
    float fade = Math.min(1f, effects.getShieldRemainingMs() / 450f);
    float pulse = 0.64f + 0.12f * (float) Math.sin(elapsed / 260d);
    float flash = (1f - opening) * 0.25f;
    float colour = batch.getPackedColor();
    int source = batch.getBlendSrcFunc(), destination = batch.getBlendDstFunc();
    int sourceAlpha = batch.getBlendSrcFuncAlpha(), destinationAlpha = batch.getBlendDstFuncAlpha();
    try {
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
      batch.setColor(0.25f, 0.8f, 1f, (pulse + flash) * fade);
      ring(batch, centre, diameter, (elapsed % 6000) * 0.06f);
      batch.setColor(0.8f, 0.96f, 1f, (0.35f + flash) * fade);
      ring(batch, centre, diameter * 0.94f, 60f - (elapsed % 9000) * 0.04f);
    } finally {
      batch.setPackedColor(colour);
      batch.setBlendFunctionSeparate(source, destination, sourceAlpha, destinationAlpha);
    }
  }

  private void ring(SpriteBatch batch, Vector2 centre, float diameter, float angle) {
    batch.draw(
        shieldTexture,
        centre.x - diameter / 2f,
        centre.y - diameter / 2f,
        diameter / 2f,
        diameter / 2f,
        diameter,
        diameter,
        1f,
        1f,
        angle,
        0,
        0,
        128,
        128,
        false,
        false);
  }

  private Texture createShieldTexture() {
    Pixmap pixels = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
    try {
      pixels.setBlending(Pixmap.Blending.None);
      for (int y = 0; y < 128; y++) {
        for (int x = 0; x < 128; x++) {
          float dx = (x - 63.5f) / 64f;
          float dy = (y - 63.5f) / 64f;
          float radius = (float) Math.sqrt(dx * dx + dy * dy);
          float ring = Math.max(0f, 1f - Math.abs(radius - 0.86f) / 0.085f);
          float glow = Math.max(0f, 1f - Math.abs(radius - 0.86f) / 0.14f) * 0.2f;
          float arc = 0.55f + 0.45f * (float) Math.pow(Math.cos(Math.atan2(dy, dx)), 8);
          pixels.setColor(1f, 1f, 1f, Math.min(1f, ring * arc + glow));
          pixels.drawPixel(x, y);
        }
      }
      Texture texture = new Texture(pixels);
      texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
      return texture;
    } finally {
      pixels.dispose();
    }
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() + 0.09f;
  }

  @Override
  public void dispose() {
    if (disposed) return;
    disposed = true;
    if (shieldTexture != null) {
      shieldTexture.dispose();
      shieldTexture = null;
    }
    super.dispose();
  }
}
