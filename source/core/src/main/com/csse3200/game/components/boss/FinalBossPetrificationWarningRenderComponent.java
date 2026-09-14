package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Temporary placeholder renderer for the Stage 1 petrification warning.
 *
 * <p>Displays a flashing red circle at a fixed captured world position. This can later be replaced
 * by permanent VFX without changing the petrification gameplay logic.
 */
public class FinalBossPetrificationWarningRenderComponent extends RenderComponent {
  private static final int TEXTURE_SIZE = 64;
  private static final int RING_THICKNESS = 5;
  private static final long FLASH_INTERVAL_MS = 160L;
  private static final float DIM_ALPHA = 0.25f;

  private final Vector2 warningCentre = new Vector2();

  private boolean visible;
  private float warningRadius;
  private Texture ringTexture;

  /** Displays the warning circle at the supplied fixed world position. */
  public void showAt(Vector2 centre, float radius) {
    if (centre == null) {
      throw new IllegalArgumentException("Warning centre must not be null");
    }
    if (radius < 0f) {
      throw new IllegalArgumentException("Warning radius must be non-negative");
    }

    warningCentre.set(centre);
    warningRadius = radius;
    visible = true;
  }

  /** Hides the warning circle. */
  public void hide() {
    visible = false;
  }

  /** Returns whether the warning circle is currently visible. */
  public boolean isVisible() {
    return visible;
  }

  /** Returns the fixed world position currently marked by the warning. */
  public Vector2 getWarningCentre() {
    return warningCentre.cpy();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (!visible || warningRadius <= 0f) {
      return;
    }

    ensureTexture();

    Color batchColour = batch.getColor();
    float oldR = batchColour.r;
    float oldG = batchColour.g;
    float oldB = batchColour.b;
    float oldA = batchColour.a;

    float alpha = getFlashAlpha();
    batch.setColor(1f, 1f, 1f, alpha);

    float diameter = warningRadius * 2f;
    batch.draw(
        ringTexture,
        warningCentre.x - warningRadius,
        warningCentre.y - warningRadius,
        diameter,
        diameter);

    batch.setColor(oldR, oldG, oldB, oldA);
  }

  /**
   * Draw warning areas before normal entities so that the circle appears on the floor beneath the
   * player and enemies.
   */
  @Override
  public float getZIndex() {
    return Float.NEGATIVE_INFINITY;
  }

  @Override
  public void dispose() {
    super.dispose();

    if (ringTexture != null) {
      ringTexture.dispose();
      ringTexture = null;
    }
  }

  /**
   * Creates the temporary ring texture lazily on the render thread.
   *
   * <p>No external art asset is required.
   */
  private void ensureTexture() {
    if (ringTexture != null) {
      return;
    }

    Pixmap pixmap = new Pixmap(TEXTURE_SIZE, TEXTURE_SIZE, Pixmap.Format.RGBA8888);

    pixmap.setBlending(Pixmap.Blending.None);

    pixmap.setColor(0f, 0f, 0f, 0f);
    pixmap.fill();

    int centre = TEXTURE_SIZE / 2;
    int outerRadius = centre - 2;
    int innerRadius = Math.max(0, outerRadius - RING_THICKNESS);

    pixmap.setColor(1f, 0f, 0f, 1f);
    pixmap.fillCircle(centre, centre, outerRadius);

    pixmap.setColor(0f, 0f, 0f, 0f);
    pixmap.fillCircle(centre, centre, innerRadius);

    ringTexture = new Texture(pixmap);
    pixmap.dispose();
  }

  private float getFlashAlpha() {
    GameTime time = ServiceLocator.getTimeSource();
    if (time == null) {
      return 1f;
    }

    long flashStep = time.getTime() / FLASH_INTERVAL_MS;
    return flashStep % 2L == 0L ? 1f : DIM_ALPHA;
  }
}
