package com.csse3200.game.components.boss;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FileTextureData;

/** Removes only the supplied GIF's exact solid background when a shared texture is loaded. */
final class FinalBossTornadoTextureData extends FileTextureData {
  private static final int BACKGROUND_RGBA = 0x161E23FF;

  FinalBossTornadoTextureData(FileHandle file) {
    super(file, null, Pixmap.Format.RGBA8888, false);
  }

  @Override
  public Pixmap consumePixmap() {
    Pixmap pixmap = super.consumePixmap();
    makeBackgroundTransparent(pixmap);
    return pixmap;
  }

  /** Preserve all RGB artwork values; only exact background pixels lose their alpha. */
  static void makeBackgroundTransparent(Pixmap pixmap) {
    Pixmap.Blending previous = pixmap.getBlending();
    pixmap.setBlending(Pixmap.Blending.None);
    try {
      for (int y = 0; y < pixmap.getHeight(); y++) {
        for (int x = 0; x < pixmap.getWidth(); x++) {
          if (pixmap.getPixel(x, y) == BACKGROUND_RGBA) {
            pixmap.drawPixel(x, y, BACKGROUND_RGBA & 0xFFFFFF00);
          }
        }
      }
    } finally {
      pixmap.setBlending(previous);
    }
  }
}
