package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;

/** Original uploaded PNGs, loaded once by the room and sliced in memory. */
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
          "burst"
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
    TextureRegion[] frames = new TextureRegion[count];
    for (int i = 0; i < count; i++) {
      int cell = start + i;
      frames[i] =
          new TextureRegion(
              texture, cell % columns * width, cell / columns * height, width, height);
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
