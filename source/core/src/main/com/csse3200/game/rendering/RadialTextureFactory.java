package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * Builds the soft circular textures that area visuals draw: a square texture, clear outside the
 * circle, whose alpha inside it is whatever the caller's profile says.
 *
 * <p>Always white, so one builder serves every circle in the game; the colour arrives from the
 * batch at draw time. Only the falloff differs between a spell's area disc, a boss aura and
 * anything else drawn on the floor, so only the falloff is worth passing in.
 */
public final class RadialTextureFactory {

  /** The alpha of a pixel at a distance from the centre, where 0 is the centre and 1 the edge. */
  @FunctionalInterface
  public interface AlphaProfile {
    float alphaAt(float distance);
  }

  /**
   * @param size width and height of the texture, in pixels
   * @param profile alpha at each distance from the centre
   * @return a white circular texture, linearly filtered. The caller owns it and must dispose it.
   */
  public static Texture create(int size, AlphaProfile profile) {
    Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
    try {
      pixmap.setBlending(Pixmap.Blending.None);
      float half = size / 2f;
      for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {
          // Pixel centres, so the circle is symmetric rather than half a pixel off.
          float dx = (x + 0.5f - half) / half;
          float dy = (y + 0.5f - half) / half;
          float distance = (float) Math.sqrt(dx * dx + dy * dy);
          if (distance >= 1f) {
            continue;
          }
          pixmap.setColor(1f, 1f, 1f, profile.alphaAt(distance));
          pixmap.drawPixel(x, y);
        }
      }
      Texture texture = new Texture(pixmap);
      texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
      return texture;
    } finally {
      pixmap.dispose();
    }
  }

  private RadialTextureFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
