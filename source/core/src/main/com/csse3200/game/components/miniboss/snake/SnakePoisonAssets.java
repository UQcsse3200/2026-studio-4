package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.services.ResourceService;
import java.util.ArrayList;
import java.util.List;

/** Original poison sheets, owned and unloaded by the room resource service. */
public final class SnakePoisonAssets {
  private static final String DIRECTORY = "images/snake-miniboss/poison/";
  private static final String FORMING = DIRECTORY + "PoisonProjectile_forming_spritesheet.png";
  private static final String FLYING = DIRECTORY + "PoisonProjectile_flying_spritesheet.png";
  private static final String FADING = DIRECTORY + "PoisonProjectile_fading_spritesheet.png";
  private static final int CELL_SIZE = 64;

  private SnakePoisonAssets() {}

  /** Returns a fresh array for room preloading without exposing shared mutable state. */
  public static String[] paths() {
    return new String[] {FORMING, FLYING, FADING};
  }

  static Frames load(ResourceService resources) {
    return new Frames(
        frames(resources, FORMING, 10), frames(resources, FLYING, 4), frames(resources, FADING, 8));
  }

  private static List<TextureRegion> frames(ResourceService resources, String path, int count) {
    Texture texture = resources.getAsset(path, Texture.class);
    int columns = texture.getWidth() / CELL_SIZE;
    List<TextureRegion> frames = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      frames.add(
          new TextureRegion(
              texture, (i % columns) * CELL_SIZE, (i / columns) * CELL_SIZE, CELL_SIZE, CELL_SIZE));
    }
    return List.copyOf(frames);
  }

  record Frames(
      List<TextureRegion> forming, List<TextureRegion> flying, List<TextureRegion> fading) {}
}
