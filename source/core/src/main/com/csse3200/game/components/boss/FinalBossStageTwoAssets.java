package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stage 2 artwork, owned by the room resource service and sliced without changing source images.
 */
public final class FinalBossStageTwoAssets {
  private static final String ROOT = "images/final-boss/stage2/";
  private static final String TRANSFORM = ROOT + "transform/01.png";
  private static final String SHIELD = ROOT + "shield/fire_circles_50x50px.png";
  private static final int FIREBALL_FRAMES = 5;
  private static final int IMPACT_FRAMES = 7;

  private FinalBossStageTwoAssets() {}

  /** Resource paths loaded and unloaded with the room. */
  public static String[] paths() {
    List<String> paths = new ArrayList<>();
    paths.add(TRANSFORM);
    paths.add(SHIELD);
    for (int i = 1; i <= FIREBALL_FRAMES; i++) {
      paths.add(numberedPath("fireball", i));
    }
    for (int i = 1; i <= IMPACT_FRAMES; i++) {
      paths.add(numberedPath("impact", i));
    }
    return paths.toArray(String[]::new);
  }

  /** The orange burst occupies the first nine cells of the sixth row. */
  static TextureRegion[] transformFrames() {
    return sheet(TRANSFORM, 64, 11, 55, 9);
  }

  /** The final three cells are empty and must not introduce gaps in the shield loop. */
  static TextureRegion[] shieldFrames() {
    return sheet(SHIELD, 50, 8, 0, 61);
  }

  /** Uses only the flying fireball frames, excluding the supplied trailing disappearance frames. */
  static TextureRegion[] fireballFrames() {
    return individualFrames("fireball", FIREBALL_FRAMES);
  }

  static TextureRegion[] impactFrames() {
    return individualFrames("impact", IMPACT_FRAMES);
  }

  private static Texture texture(String path) {
    Texture texture = ServiceLocator.getResourceService().getAsset(path, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    return texture;
  }

  private static TextureRegion[] sheet(
      String path, int cellSize, int columns, int start, int count) {
    Texture texture = texture(path);
    TextureRegion[] frames = new TextureRegion[count];
    for (int i = 0; i < count; i++) {
      int cell = start + i;
      frames[i] =
          new TextureRegion(
              texture, cell % columns * cellSize, cell / columns * cellSize, cellSize, cellSize);
    }
    return frames;
  }

  private static TextureRegion[] individualFrames(String folder, int count) {
    TextureRegion[] frames = new TextureRegion[count];
    for (int i = 0; i < count; i++) {
      frames[i] = new TextureRegion(texture(numberedPath(folder, i + 1)));
    }
    return frames;
  }

  private static String numberedPath(String folder, int index) {
    return String.format(Locale.ROOT, "%s%s/%03d.png", ROOT, folder, index);
  }

  /** Selects a looping frame or retains the final frame of a completed one-shot effect. */
  static TextureRegion frame(TextureRegion[] frames, float elapsed, float duration, boolean loop) {
    if (frames.length == 0) {
      throw new IllegalArgumentException("Animation frame count must be positive");
    }
    float time = Math.max(0f, elapsed);
    int index =
        loop && duration > 0f
            ? (int) (time / duration * frames.length) % frames.length
            : FinalBossVisualAssets.once(time, duration, frames.length);
    return frames[index];
  }
}
