package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.consumables.InstantHealingPotion;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Expanding pulse and rising crosses when an instant healing potion succeeds. */
public class HealingPotionFeedbackComponent extends RenderComponent {
  private static final float DURATION = 1.2f;
  private Texture pixel;
  private float remaining;
  private float intensity = 1f;
  private boolean disposed;
  private CombatStatsComponent stats;
  private int previousHealth;
  private int lastHealthIncrease;
  private String healingText;
  private BitmapFont font;
  private float fontHeight;

  @Override
  public void create() {
    super.create();
    stats = entity.getComponent(CombatStatsComponent.class);
    if (stats != null) previousHealth = stats.getHealth();
    entity.getEvents().addListener("updateHealth", this::onHealthUpdated);
    entity.getEvents().addListener(ConsumableEffectComponent.USED, this::onItemUsed);
    entity.getEvents().addListener("entityDied", () -> remaining = 0f);
  }

  private void onHealthUpdated(int health) {
    lastHealthIncrease = Math.max(0, health - previousHealth);
    previousHealth = health;
  }

  private void onItemUsed(String id) {
    if (!disposed
        && stats != null
        && !stats.isDead()
        && InstantHealingPotion.isHealingPotionId(id)) {
      InstantHealingPotion potion = (InstantHealingPotion) ItemCatalog.create(id, 1);
      intensity = Math.min(1.35f, 0.85f + potion.getHealing() / 200f);
      // Healing emits updateHealth before USED; use the actual gain after the health cap.
      healingText = "+" + lastHealthIncrease;
      remaining = DURATION;
    }
  }

  @Override
  public void update() {
    if (disposed || remaining <= 0f) return;
    var time = ServiceLocator.getTimeSource();
    if (time == null) return;
    float delta = time.getDeltaTime();
    if (Float.isFinite(delta) && delta > 0f) {
      remaining = Math.max(0f, remaining - delta);
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (disposed || remaining <= 0f || stats == null || stats.isDead() || isRepeatPass()) return;
    ensurePixel();
    float progress = 1f - remaining / DURATION;
    Vector2 centre = entity.getCenterPosition();
    float size = Math.max(entity.getScale().x, entity.getScale().y);
    float originalColour = batch.getPackedColor();
    int source = batch.getBlendSrcFunc(), destination = batch.getBlendDstFunc();
    int sourceAlpha = batch.getBlendSrcFuncAlpha(), destinationAlpha = batch.getBlendDstFuncAlpha();
    try {
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
      // A fast outward pulse supplies the impact; the slower crosses carry the recovery.
      if (progress < 0.3f) {
        float pulse = progress / 0.3f;
        float radius = size * intensity * (0.18f + 0.6f * (1f - (1f - pulse) * (1f - pulse)));
        batch.setColor(1f, 0.3f, 0.38f, (1f - pulse) * 0.8f);
        for (int i = 0; i < 32; i++) {
          float angle = i * 360f / 32f;
          float radians = (float) Math.toRadians(angle);
          float length = radius * 0.21f;
          float width = size * 0.035f * (1f - pulse * 0.5f);
          float x = centre.x + (float) Math.cos(radians) * radius;
          float y = centre.y + (float) Math.sin(radians) * radius;
          batch.draw(
              pixel,
              x - length / 2f,
              y - width / 2f,
              length / 2f,
              width / 2f,
              length,
              width,
              1f,
              1f,
              angle + 90f,
              0,
              0,
              1,
              1,
              false,
              false);
        }
      }
      float alpha = Math.min(1f, remaining / DURATION * 1.5f);
      float pop = 1f + 0.35f * (float) Math.sin(Math.min(1f, progress / 0.2f) * Math.PI);
      float rise = size * (0.15f + progress * 0.75f);
      for (int i = -1; i <= 1; i++) {
        float x = centre.x + i * size * (0.22f + progress * 0.2f);
        float y = centre.y + rise + (i == 0 ? size * 0.18f : 0f);
        float length = size * intensity * (i == 0 ? 0.38f : 0.23f) * pop;
        batch.setColor(1f, 0.12f, 0.22f, alpha * 0.7f);
        cross(batch, x, y, length, length * 0.27f);
        if (i == 0) {
          batch.setColor(1f, 0.85f, 0.88f, alpha);
          cross(batch, x, y, length * 0.78f, length * 0.12f);
        }
      }
      drawHealingNumber(batch, centre, size, rise, pop, alpha);
    } finally {
      batch.setPackedColor(originalColour);
      batch.setBlendFunctionSeparate(source, destination, sourceAlpha, destinationAlpha);
    }
  }

  private void drawHealingNumber(
      SpriteBatch batch, Vector2 centre, float size, float rise, float pop, float alpha) {
    if (font == null) {
      font = new BitmapFont(Gdx.files.internal("flat-earth/skin/fonts/pixel_16.fnt"));
      // World coordinates are smaller than a screen pixel; integer rounding hides glyphs.
      font.setUseIntegerPositions(false);
      fontHeight = Math.max(1f, font.getCapHeight());
    }
    font.getData().setScale(size * 0.33f * pop / fontHeight);
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    batch.setColor(Color.WHITE);
    float width = size * 2.2f;
    float x = centre.x - width / 2f;
    float y = centre.y + rise + size * 0.8f;
    float shadow = size * 0.025f;
    font.setColor(0f, 0f, 0f, alpha * 0.85f);
    font.draw(batch, healingText, x + shadow, y - shadow, width, Align.center, false);
    font.setColor(1f, 0.12f, 0.22f, alpha);
    font.draw(batch, healingText, x, y, width, Align.center, false);
  }

  private void cross(SpriteBatch batch, float x, float y, float length, float width) {
    batch.draw(pixel, x - length / 2f, y - width / 2f, length, width);
    batch.draw(pixel, x - width / 2f, y - length / 2f, width, length);
  }

  private void ensurePixel() {
    if (pixel != null) return;
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    try {
      pixmap.setColor(Color.WHITE);
      pixmap.fill();
      pixel = new Texture(pixmap);
    } finally {
      pixmap.dispose();
    }
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() + 0.1f;
  }

  @Override
  public void dispose() {
    if (disposed) return;
    disposed = true;
    remaining = 0f;
    if (font != null) {
      font.dispose();
      font = null;
    }
    if (pixel != null) {
      pixel.dispose();
      pixel = null;
    }
    super.dispose();
  }
}
