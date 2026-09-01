package com.csse3200.game.areas.terrain;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Provides named access to equally sized tiles within a texture sheet. */
public class TileSheet {
  private final Texture texture;
  private final int tileSize;
  private final int columns;
  private final int rows;

  /** Creates a tile-sheet view over a texture. */
  public TileSheet(Texture texture, int tileSize) {
    if (tileSize <= 0
        || texture.getWidth() % tileSize != 0
        || texture.getHeight() % tileSize != 0) {
      throw new IllegalArgumentException("Texture dimensions must be divisible by tile size");
    }
    this.texture = texture;
    this.tileSize = tileSize;
    columns = texture.getWidth() / tileSize;
    rows = texture.getHeight() / tileSize;
  }

  /** Gets one tile, using zero-indexed coordinates from the sheet's top-left corner. */
  public TextureRegion tile(int column, int row) {
    if (column < 0 || column >= columns || row < 0 || row >= rows) {
      throw new IllegalArgumentException("Tile coordinate is outside the tile sheet");
    }
    return new TextureRegion(texture, column * tileSize, row * tileSize, tileSize, tileSize);
  }

  public int getColumns() {
    return columns;
  }

  public int getRows() {
    return rows;
  }
}
