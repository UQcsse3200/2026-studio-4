package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.services.ServiceLocator;
import java.util.Arrays;

/** Original sprite sheets selected for Grandpa. Textures are owned by the room resource service. */
public enum FinalBossVisualAssets {
  GRANDPA("grandpa.png", 16, 5, 10),
  WIZARD("wizard.png", 192, 4, 14),
  TRANSFORM("transform.png", 32, 6, 12),
  SPAWN("summon-spawn.png", 32, 6, 12),
  SHIELD("shield.png", 72, 8, 8),
  SHIELD_HIT("shield-hit.png", 72, 4, 4),
  ATTACK_ALERT("attack-alert.png", 1024, 8, 8),
  BOMB_IDLE("bomb-idle.png", 50, 3, 3),
  BOMB_EXPLOSION("bomb-explosion.png", 50, 11, 11),
  STONE("stone.png", 72, 8, 8);

  private final String path;
  private final int cellSize;
  private final int columns;
  private final int count;

  FinalBossVisualAssets(String filename, int cellSize, int columns, int count) {
    this.path = "images/final-boss/" + filename;
    this.cellSize = cellSize;
    this.columns = columns;
    this.count = count;
  }

  /** Resource paths loaded and unloaded with each room. */
  public static String[] paths() {
    return Arrays.stream(values()).map(asset -> asset.path).toArray(String[]::new);
  }

  /** Slices the original sheet in memory, preserving the uploaded PNG bytes. */
  public TextureRegion[] loadFrames() {
    Texture texture = ServiceLocator.getResourceService().getAsset(path, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextureRegion[] frames = new TextureRegion[count];
    for (int i = 0; i < count; i++) {
      frames[i] =
          new TextureRegion(
              texture, (i % columns) * cellSize, (i / columns) * cellSize, cellSize, cellSize);
    }
    return frames;
  }

  /** Selects a frame in a one-shot sequence, retaining the last frame at its endpoint. */
  static int once(float elapsed, float duration, int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("Animation frame count must be positive");
    }

    if (duration <= 0f) {
      return count - 1;
    }

    int frame = (int) (elapsed / duration * count);
    return Math.clamp(frame, 0, count - 1);
  }
}
