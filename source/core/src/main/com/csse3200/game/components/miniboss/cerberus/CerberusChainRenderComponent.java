package com.csse3200.game.components.miniboss.cerberus;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;

public class CerberusChainRenderComponent extends RenderComponent {
  private static final int SEGMENTS = 40;
  private static final int SEARCH_STEPS = 14;
  private static final float CHAIN_WIDTH = 0.10f;
  private static final float LINK_LENGTH = 0.20f;

  private final TextureRegion texture;
  private final float chainLength;
  private CerberusChainComponent chain;

  public CerberusChainRenderComponent(TextureRegion texture, float chainLength) {
    if (texture == null || !Float.isFinite(chainLength) || chainLength <= 0f) {
      throw new IllegalArgumentException("Chain texture and positive finite length are required");
    }
    this.texture = texture;
    this.chainLength = chainLength;
  }

  @Override
  public void create() {
    chain = entity.getComponent(CerberusChainComponent.class);
    if (chain == null) {
      throw new IllegalStateException("CerberusChainComponent is required");
    }
    super.create();
  }

  /**
   * Builds a sagging path with approximately constant length. If the endpoints exceed that length,
   * draws straight as a visual fallback.
   */
  public Vector2[] getChainPoints() {
    Vector2 start = chain.getWallAnchor();
    Vector2 end = chain.getBodyAttachment();

    float low = 0f;
    float high = chainLength;

    if (start.dst(end) >= chainLength) {
      return buildPath(start, end, 0f);
    }

    for (int i = 0; i < SEARCH_STEPS; i++) {
      float sag = (low + high) / 2f;
      if (pathLength(buildPath(start, end, sag)) < chainLength) {
        low = sag;
      } else {
        high = sag;
      }
    }

    return buildPath(start, end, (low + high) / 2f);
  }

  private Vector2[] buildPath(Vector2 start, Vector2 end, float sag) {
    Vector2[] points = new Vector2[SEGMENTS + 1];

    for (int i = 0; i <= SEGMENTS; i++) {
      float t = (float) i / SEGMENTS;
      points[i] =
          new Vector2(
              start.x + (end.x - start.x) * t,
              start.y + (end.y - start.y) * t - 4f * sag * t * (1f - t));
    }

    return points;
  }

  private float pathLength(Vector2[] points) {
    float length = 0f;
    for (int i = 1; i < points.length; i++) {
      length += points[i - 1].dst(points[i]);
    }
    return length;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    Vector2[] points = getChainPoints();
    float length = pathLength(points);
    if (length <= 0.0001f) {
      return;
    }

    int links = Math.max(1, (int) Math.ceil(length / LINK_LENGTH));
    float spacing = length / links;

    Vector2 previous = points[0];
    for (int i = 1; i <= links; i++) {
      Vector2 next = pointAtDistance(points, Math.min(length, i * spacing));
      drawLink(batch, previous, next);
      previous = next;
    }
  }

  private Vector2 pointAtDistance(Vector2[] points, float distance) {
    float remaining = distance;

    for (int i = 1; i < points.length; i++) {
      Vector2 start = points[i - 1];
      Vector2 end = points[i];
      float length = start.dst(end);

      if (length > 0f && remaining <= length) {
        return start.cpy().lerp(end, remaining / length);
      }
      remaining -= length;
    }

    return points[points.length - 1].cpy();
  }

  private void drawLink(SpriteBatch batch, Vector2 start, Vector2 end) {
    Vector2 direction = end.cpy().sub(start);
    float length = direction.len();
    if (length <= 0.0001f) {
      return;
    }

    // The source chain segment points vertically upward.
    batch.draw(
        texture,
        start.x - CHAIN_WIDTH / 2f,
        start.y,
        CHAIN_WIDTH / 2f,
        0f,
        CHAIN_WIDTH,
        length,
        1f,
        1f,
        direction.angleDeg() - 90f);
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() - 0.01f;
  }
}
