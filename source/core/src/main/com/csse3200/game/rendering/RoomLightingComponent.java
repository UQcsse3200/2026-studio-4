package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;

/**
 * Darkens a room and lets its lights shine through, giving a candle-lit, pixel-art look.
 *
 * <p>The room is covered with small square cells of darkness. Each cell's opacity is the room's
 * darkness reduced by the light reaching it, then stepped into a few bands so light pools have crisp
 * pixel-art rings instead of a smooth gradient. A faint additive glow in each light's colour then
 * warms the lit areas. The player carries a small lantern light once the room is entered.
 *
 * <p>Sits first in render layer 2, so it covers terrain, furniture and characters but not flames,
 * floating effects or the HUD.
 */
public class RoomLightingComponent extends RenderComponent {
  private static final int LAYER = 2;
  /** Cell edge in world units: 4 sprite pixels. */
  static final float CELL = 0.125f;
  /** Number of brightness bands; fewer reads as chunkier, more retro lighting. */
  static final int BANDS = 6;
  private static final float TINT_STRENGTH = 0.16f;
  private static final float PLAYER_LIGHT_RADIUS = 2.2f;
  private static final Color PLAYER_LIGHT_COLOR = new Color(1f, 0.85f, 0.6f, 1f);

  private final float darkness;
  private final Color shadowColor;
  private final List<LightSource> lights;
  private final float minX;
  private final float minY;
  private final float maxX;
  private final float maxY;

  private LightSource playerLight;
  private Entity player;
  private Texture pixel;
  private Texture glow;
  private float elapsed;

  /**
   * @param darkness opacity of the dark in unlit areas, 0..1
   * @param shadowColor colour of the dark (a hint of blue reads as night)
   * @param lights the room's lights
   * @param min bottom-left of the area to darken, world units
   * @param max top-right of the area to darken, world units
   */
  public RoomLightingComponent(
      float darkness, Color shadowColor, List<LightSource> lights, Vector2 min, Vector2 max) {
    this.darkness = MathUtils.clamp(darkness, 0f, 1f);
    this.shadowColor = shadowColor.cpy();
    this.lights = new ArrayList<>(lights);
    this.minX = min.x;
    this.minY = min.y;
    this.maxX = max.x;
    this.maxY = max.y;
  }

  @Override
  public void create() {
    super.create();
    Pixmap white = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    white.setColor(Color.WHITE);
    white.fill();
    pixel = new Texture(white);
    white.dispose();
    glow = RadialTextureFactory.create(64, d -> (1f - d) * (1f - d));
    entity.getEvents().addListener("RoomCreated", this::attachPlayerLight);
  }

  /** Gives the player a small lantern so they are never walking blind. */
  void attachPlayerLight(Entity newPlayer) {
    player = newPlayer;
    if (playerLight == null) {
      playerLight = new LightSource(0f, 0f, PLAYER_LIGHT_RADIUS, PLAYER_LIGHT_COLOR, 0.15f);
      lights.add(playerLight);
    }
  }

  public List<LightSource> getLights() {
    return lights;
  }

  @Override
  public int getLayer() {
    return LAYER;
  }

  @Override
  public float getZIndex() {
    return -Float.MAX_VALUE;
  }

  /**
   * Opacity of the darkness at a point, already stepped into bands.
   *
   * @param seconds time, for flicker
   */
  public float shadeAt(float x, float y, float seconds) {
    return shadeWithin(x, y, reaches(seconds));
  }

  private float[] reaches(float seconds) {
    float[] reach = new float[lights.size()];
    for (int i = 0; i < reach.length; i++) {
      LightSource light = lights.get(i);
      reach[i] = light.getRadius() * light.intensity(seconds);
    }
    return reach;
  }

  private float shadeWithin(float x, float y, float[] reach) {
    // Combine lights like overlapping torches: each removes a share of the remaining dark.
    float remainingDark = 1f;
    for (int i = 0; i < reach.length; i++) {
      remainingDark *= 1f - lights.get(i).contributionWithin(x, y, reach[i]);
    }
    float alpha = darkness * remainingDark;
    return MathUtils.ceil(alpha * BANDS - 0.001f) / (float) BANDS;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (pixel == null) {
      return;
    }
    // Absolute time rather than accumulated deltas, so extra render passes don't speed up flicker.
    elapsed =
        ServiceLocator.getTimeSource() == null
            ? 0f
            : ServiceLocator.getTimeSource().getTime() / 1000f;
    if (player != null && playerLight != null) {
      Vector2 centre = player.getCenterPosition();
      playerLight.setPosition(centre.x, centre.y);
    }

    Color previous = batch.getColor().cpy();
    float[] reach = reaches(elapsed);

    for (float y = minY; y < maxY; y += CELL) {
      for (float x = minX; x < maxX; x += CELL) {
        float alpha = shadeWithin(x + CELL / 2f, y + CELL / 2f, reach);
        if (alpha <= 0f) {
          continue;
        }
        batch.setColor(shadowColor.r, shadowColor.g, shadowColor.b, alpha);
        batch.draw(pixel, x, y, CELL, CELL);
      }
    }

    // Warm the lit areas with each light's colour.
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    for (int i = 0; i < reach.length; i++) {
      LightSource light = lights.get(i);
      Color c = light.getColor();
      float r = reach[i];
      batch.setColor(c.r, c.g, c.b, TINT_STRENGTH * darkness);
      batch.draw(glow, light.getX() - r, light.getY() - r, r * 2f, r * 2f);
    }
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    batch.setColor(previous);
  }

  @Override
  public void dispose() {
    if (pixel != null) {
      pixel.dispose();
      pixel = null;
    }
    if (glow != null) {
      glow.dispose();
      glow = null;
    }
    super.dispose();
  }
}
