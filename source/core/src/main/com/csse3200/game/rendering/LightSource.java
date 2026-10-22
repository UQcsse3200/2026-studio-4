package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

/**
 * A point light for {@link RoomLightingComponent}. Positions are in world units. A light can follow
 * an entity by having its position updated each frame with {@link #setPosition}.
 */
public class LightSource {
  private float x;
  private float y;
  private final float radius;
  private final Color color;
  private final float flicker;
  private final float phase;

  /**
   * @param x centre x, world units
   * @param y centre y, world units
   * @param radius distance at which the light fades out completely, world units
   * @param color tint of the light
   * @param flicker 0 for steady, up to 1 for a lively flame
   */
  public LightSource(float x, float y, float radius, Color color, float flicker) {
    this.x = x;
    this.y = y;
    this.radius = radius;
    this.color = color.cpy();
    this.flicker = MathUtils.clamp(flicker, 0f, 1f);
    // Different phase per light so candles don't flicker in unison.
    this.phase = (x * 12.9898f + y * 78.233f) % MathUtils.PI2;
  }

  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
  }

  public float getX() {
    return x;
  }

  public float getY() {
    return y;
  }

  public float getRadius() {
    return radius;
  }

  public Color getColor() {
    return color;
  }

  /** Brightness multiplier at a time (seconds): 1 for steady lights, wobbling for flames. */
  public float intensity(float seconds) {
    if (flicker <= 0f) {
      return 1f;
    }
    // A few incommensurate sines read as an irregular flame without needing randomness.
    float wobble =
        0.5f * MathUtils.sin(seconds * 7.3f + phase)
            + 0.3f * MathUtils.sin(seconds * 13.1f + phase * 2f)
            + 0.2f * MathUtils.sin(seconds * 23.7f + phase * 3f);
    return 1f - flicker * 0.05f * (1f + wobble);
  }

  /**
   * How much this light brightens a point, 0..1: full at the centre, fading smoothly to 0 at the
   * radius.
   */
  public float contributionAt(float px, float py, float seconds) {
    return contributionWithin(px, py, radius * intensity(seconds));
  }

  /** As {@link #contributionAt} with the flickered reach already worked out (once per frame). */
  float contributionWithin(float px, float py, float reach) {
    float dx = px - x;
    float dy = py - y;
    float distanceSq = dx * dx + dy * dy;
    if (distanceSq >= reach * reach) {
      return 0f;
    }
    float t = 1f - (float) Math.sqrt(distanceSq) / reach;
    return t * t * (3f - 2f * t); // smoothstep
  }
}
