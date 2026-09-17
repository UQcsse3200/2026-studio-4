package com.csse3200.game.components.miniboss.cerberus;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Draws a pulsing red aura around a living, enraged Cerberus head. */
public class CerberusEnrageVisualComponent extends RenderComponent {
  private static final int TEXTURE_SIZE = 64;
  private static final float SIZE_MULTIPLIER = 1.6f;
  private static final float PULSE_PERIOD = 1.2f;

  private final CerberusPhaseComponent phase;

  private CombatStatsComponent stats;
  private Texture auraTexture;
  private boolean enraged;
  private boolean stopped;
  private float elapsed;

  public CerberusEnrageVisualComponent(CerberusPhaseComponent phase) {
    this.phase = phase;
  }

  @Override
  public void create() {
    stats = entity.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("enragePhaseStarted", this::startEnrage);
    entity.getEvents().addListener("entityDied", this::stop);

    if (stats.isDead()) {
      stop();
    } else if (phase.getCurrentPhase() == 2) {
      startEnrage();
    }

    super.create();
  }

  private void startEnrage() {
    if (stopped || stats.isDead() || enraged) {
      return;
    }

    enraged = true;
    elapsed = 0f;
  }

  private void stop() {
    stopped = true;
    enraged = false;
  }

  public boolean isAuraActive() {
    return enraged && !stopped && stats != null && !stats.isDead();
  }

  @Override
  public void update() {
    if (!isAuraActive()) {
      return;
    }

    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (Float.isFinite(delta) && delta > 0f) {
      elapsed = (elapsed + delta) % PULSE_PERIOD;
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (!isAuraActive()) {
      return;
    }

    if (auraTexture == null) {
      createAuraTexture();
    }

    Vector2 centre = entity.getCenterPosition();
    Vector2 scale = entity.getScale();
    float width = scale.x * SIZE_MULTIPLIER;
    float height = scale.y * SIZE_MULTIPLIER;
    float opacity = 0.65f + 0.2f * (float) Math.sin(elapsed * Math.PI * 2 / PULSE_PERIOD);

    float originalColour = batch.getPackedColor();
    try {
      batch.setColor(1f, 0.08f, 0.08f, opacity);
      batch.draw(auraTexture, centre.x - width / 2f, centre.y - height / 2f, width, height);
    } finally {
      batch.setPackedColor(originalColour);
    }
  }

  private void createAuraTexture() {
    Pixmap pixmap = new Pixmap(TEXTURE_SIZE, TEXTURE_SIZE, Pixmap.Format.RGBA8888);

    try {
      pixmap.setBlending(Pixmap.Blending.None);

      for (int y = 0; y < TEXTURE_SIZE; y++) {
        for (int x = 0; x < TEXTURE_SIZE; x++) {
          float dx = (x + 0.5f - TEXTURE_SIZE / 2f) / (TEXTURE_SIZE / 2f);
          float dy = (y + 0.5f - TEXTURE_SIZE / 2f) / (TEXTURE_SIZE / 2f);
          float distance = (float) Math.sqrt(dx * dx + dy * dy);

          if (distance >= 1f) {
            continue;
          }

          float alpha = 0.7f * (1f - distance);
          pixmap.setColor(1f, 1f, 1f, alpha);
          pixmap.drawPixel(x, y);
        }
      }

      auraTexture = new Texture(pixmap);
      auraTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    } finally {
      pixmap.dispose();
    }
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() - 0.01f;
  }

  @Override
  public void dispose() {
    stop();

    if (auraTexture != null) {
      auraTexture.dispose();
      auraTexture = null;
    }

    super.dispose();
  }
}
