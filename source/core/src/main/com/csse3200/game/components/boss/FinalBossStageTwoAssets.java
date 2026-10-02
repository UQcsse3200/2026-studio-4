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
  private static final String OBSTACLE = ROOT + "obstacle/crystal-icy.png";
  private static final String SHATTER = ROOT + "shatter/PixelTraps-IceShards_spritesheet.png";
  private static final String PICKUP = ROOT + "pickup/GEM 1 - BLUE - Spritesheet.png";
  private static final String PICKUP_SPARK = ROOT + "pickup/Spark - Spritesheet.png";
  private static final String ICE_AURA = ROOT + "ice-aura/ice_sparkles.png";
  private static final int FIREBALL_FRAMES = 5;
  private static final int IMPACT_FRAMES = 7;

  private FinalBossStageTwoAssets() {}

  /** Resource paths loaded and unloaded with the room. */
  public static String[] paths() {
    List<String> paths = new ArrayList<>();
    paths.add(TRANSFORM);
    paths.add(SHIELD);
    paths.add(OBSTACLE);
    paths.add(SHATTER);
    paths.add(PICKUP);
    paths.add(PICKUP_SPARK);
    paths.add(ICE_AURA);
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

  /** The cyan swirl occupies the first nine cells of the seventh row of the shared effect sheet. */
  static TextureRegion[] iceSpawnFrames() {
    return sheet(TRANSFORM, 64, 11, 66, 9);
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

  static TextureRegion[] pickupFrames() {
    return sheet(PICKUP, 18, 30, 10, 0, 10);
  }

  /** The eleventh slot is empty; use the ten visible sparkle frames. */
  static TextureRegion[] pickupSparkFrames() {
    return sheet(PICKUP_SPARK, 20, 19, 11, 0, 10);
  }

  static TextureRegion[] iceAuraFrames() {
    return sheet(ICE_AURA, 48, 64, 5, 0, 5);
  }

  /** The lower-right small crystal, preserving its original 1:2 aspect ratio. */
  static TextureRegion obstacleRegion() {
    return new TextureRegion(texture(OBSTACLE), 128, 64, 32, 64);
  }

  /**
   * Six debris frames from the supplied 2048x341 copy. These measured regions deliberately do not
   * assume the original pack's 256px cells or include the preceding intact trap animation.
   */
  static TextureRegion[] shatterFrames() {
    Texture texture = texture(SHATTER);
    int[] edges = {228, 341, 455, 569, 683, 796, 910};
    TextureRegion[] frames = new TextureRegion[edges.length - 1];
    for (int i = 0; i < frames.length; i++) {
      frames[i] = new TextureRegion(texture, edges[i], 227, edges[i + 1] - edges[i], 114);
    }
    return frames;
  }

  private static Texture texture(String path) {
    Texture texture = ServiceLocator.getResourceService().getAsset(path, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    return texture;
  }

  private static TextureRegion[] sheet(
      String path, int cellSize, int columns, int start, int count) {
    return sheet(path, cellSize, cellSize, columns, start, count);
  }

  private static TextureRegion[] sheet(
      String path, int width, int height, int columns, int start, int count) {
    Texture texture = texture(path);
    TextureRegion[] frames = new TextureRegion[count];
    for (int i = 0; i < count; i++) {
      int cell = start + i;
      frames[i] =
          new TextureRegion(
              texture, cell % columns * width, cell / columns * height, width, height);
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
