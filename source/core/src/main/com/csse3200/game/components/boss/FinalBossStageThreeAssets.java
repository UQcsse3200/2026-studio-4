package com.csse3200.game.components.boss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;

/** Supplied artwork, loaded once by the room and sliced in memory. */
public final class FinalBossStageThreeAssets {
  private static final String ROOT = "images/final-boss/stage3/";

  private FinalBossStageThreeAssets() {}

  public static String[] paths() {
    List<String> paths = new ArrayList<>();
    for (String name :
        new String[] {
          "idle",
          "run",
          "cast",
          "hit",
          "statue",
          "ice-start",
          "ice-loop",
          "ice-hit",
          "freeze-start",
          "freeze-loop",
          "freeze-end",
          "burst",
          "tornado-01",
          "tornado-02"
        }) {
      paths.add(ROOT + name + ".png");
    }
    return paths.toArray(String[]::new);
  }

  private static Texture texture(String name) {
    Texture texture =
        ServiceLocator.getResourceService().getAsset(ROOT + name + ".png", Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    return texture;
  }

  static TextureRegion[] sheet(
      String name, int width, int height, int columns, int start, int count) {
    Texture texture = texture(name);
    return slice(texture, width, height, columns, start, count);
  }

  private static TextureRegion[] slice(
      Texture texture, int width, int height, int columns, int start, int count) {
    TextureRegion[] frames = new TextureRegion[count];
    for (int i = 0; i < count; i++) {
      int cell = start + i;
      frames[i] =
          new TextureRegion(
              texture, cell % columns * width, cell / columns * height, width, height);
    }
    return frames;
  }

  /** Shares two small textures across every tornado; keying happens once, never while drawing. */
  static TextureRegion[] tornadoFrames() {
    TextureRegion[] frames = new TextureRegion[60];
    for (int sheet = 0; sheet < 2; sheet++) {
      String name = "tornado-0" + (sheet + 1);
      Texture texture = texture(name);
      if (!(texture.getTextureData() instanceof FinalBossTornadoTextureData)) {
        texture.load(new FinalBossTornadoTextureData(Gdx.files.internal(ROOT + name + ".png")));
      }
      TextureRegion[] cells = slice(texture, 80, 45, 6, 0, 30);
      System.arraycopy(cells, 0, frames, sheet * 30, cells.length);
    }
    return frames;
  }

  static TextureRegion frame(TextureRegion[] frames, float elapsed, float duration, boolean loop) {
    int index =
        loop
            ? (int) (elapsed / duration * frames.length) % frames.length
            : FinalBossVisualAssets.once(elapsed, duration, frames.length);
    return frames[Math.max(0, index)];
  }
}
