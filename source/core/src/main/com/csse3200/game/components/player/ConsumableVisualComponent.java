package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/** Player-local feedback for successful healing and the actual consumable shield lifetime. */
public class ConsumableVisualComponent extends RenderComponent {
  private static final long HEAL_DURATION_MS = 1200;
  private ConsumableEffectComponent effects;
  private CombatStatsComponent stats;
  private GameTime time;
  private long healStarted;
  private boolean healing;
  private boolean disposed;
  private Texture shieldTexture;
  private Texture plusTexture;

  @Override
  public void create() {
    effects = entity.getComponent(ConsumableEffectComponent.class);
    stats = entity.getComponent(CombatStatsComponent.class);
    time = ServiceLocator.getTimeSource();
    entity.getEvents().addListener(ConsumableEffectComponent.USED, this::onUsed);
    entity.getEvents().addListener("entityDied", () -> healing = false);
    super.create();
  }

  private void onUsed(ItemType type) {
    if (!disposed && type == ItemType.HEALTH_POTION && stats != null && !stats.isDead()) {
      healStarted = time.getTime();
      healing = true;
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (disposed || stats == null || stats.isDead()) {
      return;
    }
    boolean shield = effects != null && effects.isShielded();
    long elapsed = healing ? Math.max(0, time.getTime() - healStarted) : HEAL_DURATION_MS;
    if (elapsed >= HEAL_DURATION_MS) {
      healing = false;
    }
    if (!shield && !healing) {
      return;
    }
    Vector2 centre = entity.getCenterPosition();
    float size = Math.max(entity.getScale().x, entity.getScale().y);
    float original = batch.getPackedColor();
    try {
      if (shield) {
        if (shieldTexture == null) {
          shieldTexture = createShieldTexture();
        }
        float diameter = size * 1.35f;
        float fade = Math.min(1f, effects.getShieldRemainingMs() / 450f);
        float pulse = 0.8f + 0.12f * (float) Math.sin(time.getTime() / 240.0);
        batch.setColor(0.32f, 0.86f, 1f, pulse * fade);
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
            (time.getTime() % 6000) * 0.06f,
            0,
            0,
            128,
            128,
            false,
            false);
      }
      if (healing) {
        drawHealing(batch, centre, size, elapsed);
      }
    } finally {
      batch.setPackedColor(original);
    }
  }

  private void drawHealing(SpriteBatch batch, Vector2 centre, float size, long elapsed) {
    if (plusTexture == null) {
      plusTexture = createPlusTexture();
    }
    float progress = elapsed / (float) HEAL_DURATION_MS;
    float alpha = Math.min(1f, (1f - progress) * 2f);
    batch.setColor(1f, 0.16f, 0.22f, alpha);
    for (int i = 0; i < 3; i++) {
      float x = centre.x + (i - 1) * size * 0.38f;
      float y = centre.y + size * (0.1f + progress * 0.65f + (i == 1 ? 0.2f : 0f));
      float width = size * 0.19f;
      batch.draw(plusTexture, x - width / 2f, y, width, width);
    }
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

  private Texture createPlusTexture() {
    Pixmap pixels = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
    try {
      pixels.setColor(1f, 1f, 1f, 1f);
      pixels.fillRectangle(7, 1, 6, 18);
      pixels.fillRectangle(1, 7, 18, 6);
      return new Texture(pixels);
    } finally {
      pixels.dispose();
    }
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() + 0.02f;
  }

  @Override
  public void dispose() {
    disposed = true;
    healing = false;
    if (shieldTexture != null) {
      shieldTexture.dispose();
      shieldTexture = null;
    }
    if (plusTexture != null) {
      plusTexture.dispose();
      plusTexture = null;
    }
    super.dispose();
  }
}
