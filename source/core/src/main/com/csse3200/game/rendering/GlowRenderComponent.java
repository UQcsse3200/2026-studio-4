package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.BooleanSupplier;

/**
 * A soft, slowly pulsing halo, used to mark the magical tomes in the attic as interactable.
 *
 * <p>Draws a circle of the given radius centred on {@code entity position + offset}, lifted by
 * {@code elevation} like a {@link PropRenderComponent}, and sorts just beneath a prop with the same
 * elevation so the halo sits behind the book it surrounds. While {@code lit} reports false nothing
 * is drawn (e.g. a dungeon that cannot be entered yet).
 */
public class GlowRenderComponent extends RenderComponent {
  private static final float PULSE_PERIOD_MS = 1800f;
  private static final float MIN_ALPHA = 0.25f;
  private static final float MAX_ALPHA = 0.6f;
  private static final float SORT_NUDGE = 0.01f;
  private static final float SORT_BEHIND = 0.001f;
  private static final int TEXTURE_SIZE = 64;

  private final Color color;
  private final float radius;
  private final Vector2 offset;
  private final float elevation;
  private final BooleanSupplier lit;
  private Texture texture;

  /**
   * @param color halo colour
   * @param radius halo radius in world units
   * @param offset centre of the halo relative to the entity position, in world units
   * @param elevation height above the floor, in world units (matches the prop it surrounds)
   * @param lit whether the halo is currently shown
   */
  public GlowRenderComponent(
      Color color, float radius, Vector2 offset, float elevation, BooleanSupplier lit) {
    this.color = color.cpy();
    this.radius = radius;
    this.offset = offset.cpy();
    this.elevation = elevation;
    this.lit = lit;
  }

  @Override
  public void create() {
    super.create();
    texture = RadialTextureFactory.create(TEXTURE_SIZE, d -> (1f - d) * (1f - d));
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() + elevation * SORT_NUDGE - SORT_BEHIND;
  }

  /** Halo opacity at a point in time, oscillating between the minimum and maximum. */
  static float pulseAlpha(long timeMs) {
    float phase = (timeMs % (long) PULSE_PERIOD_MS) / PULSE_PERIOD_MS;
    float wave = 0.5f + 0.5f * MathUtils.sin(phase * MathUtils.PI2);
    return MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * wave;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (texture == null || !lit.getAsBoolean()) {
      return;
    }
    long now =
        ServiceLocator.getTimeSource() == null ? 0L : ServiceLocator.getTimeSource().getTime();
    Vector2 position = entity.getPosition();
    float cx = position.x + offset.x;
    float cy = position.y + offset.y + elevation;

    Color previous = batch.getColor().cpy();
    batch.setColor(color.r, color.g, color.b, pulseAlpha(now));
    batch.draw(texture, cx - radius, cy - radius, radius * 2, radius * 2);
    batch.setColor(previous);
  }

  @Override
  public void dispose() {
    if (texture != null) {
      texture.dispose();
      texture = null;
    }
    super.dispose();
  }
}
