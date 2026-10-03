package com.csse3200.game.components.miniboss.dragon;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.rendering.RadialTextureFactory;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Draws a pulsing blue-purple aura during the dragon's second phase. */
public class DragonEnrageVisualComponent extends RenderComponent {
  private static final float PULSE_PERIOD = 1.2f;
  private static final float SIZE_MULTIPLIER = 1.3f;

  private DragonPhaseComponent phase;
  private CombatStatsComponent stats;
  private Texture auraTexture;
  private float elapsed;
  private boolean stopped;

  @Override
  public void create() {
    phase = entity.getComponent(DragonPhaseComponent.class);
    stats = entity.getComponent(CombatStatsComponent.class);

    if (phase == null || stats == null) {
      throw new IllegalStateException("Dragon enrage visual requires phase and combat stats");
    }

    entity.getEvents().addListener("entityDied", this::stop);
    super.create();
  }

  public boolean isAuraActive() {
    return !stopped && stats != null && !stats.isDead() && phase.isEnraged();
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  public void update(float delta) {
    if (!isAuraActive() || !Float.isFinite(delta) || delta <= 0f) {
      return;
    }

    elapsed = (elapsed + delta % PULSE_PERIOD) % PULSE_PERIOD;
  }

  /** Current pulse opacity, exposed to tests in this package. */
  float getOpacity() {
    return 0.55f + 0.2f * (float) Math.sin(elapsed * Math.PI * 2 / PULSE_PERIOD);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (!isAuraActive()) {
      return;
    }

    if (auraTexture == null) {
      auraTexture = RadialTextureFactory.create(64, distance -> 0.65f * (1f - distance));
    }

    Vector2 centre = entity.getCenterPosition();
    Vector2 scale = entity.getScale();
    float width = scale.x * SIZE_MULTIPLIER;
    float height = scale.y * SIZE_MULTIPLIER;

    float originalColour = batch.getPackedColor();
    try {
      batch.setColor(0.45f, 0.25f, 1f, getOpacity());
      batch.draw(auraTexture, centre.x - width / 2f, centre.y - height / 2f, width, height);
    } finally {
      batch.setPackedColor(originalColour);
    }
  }

  private void stop() {
    stopped = true;
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
