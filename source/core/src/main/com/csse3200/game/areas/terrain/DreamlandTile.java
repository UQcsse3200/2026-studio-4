package com.csse3200.game.areas.terrain;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Curated tile palette for the prototype Fantasy Dreamland dungeon. */
public enum DreamlandTile {
  EARTH_FLOOR(0, 0),
  EARTH_FLOOR_VARIANT(2, 0),
  PURPLE_STONE_FLOOR(4, 0),
  PURPLE_STONE_FLOOR_VARIANT(5, 0),
  BLUE_STONE_FLOOR(10, 0),
  BLUE_STONE_FLOOR_VARIANT(11, 0),
  PURPLE_STONE_WALL(8, 0),
  BLUE_STONE_WALL(9, 0),
  BOOKSHELF_LEFT(0, 17),
  BOOKSHELF_MIDDLE(1, 17),
  BOOKSHELF_RIGHT(2, 17),
  CANDLE_TABLE_LEFT(10, 17),
  CANDLE_TABLE_RIGHT(11, 17),
  OPEN_BARREL(6, 23);

  public static final DreamlandTile FLOOR_STONE = PURPLE_STONE_FLOOR;
  public static final DreamlandTile WALL_STONE = PURPLE_STONE_WALL;

  private final int column;
  private final int row;

  DreamlandTile(int column, int row) {
    this.column = column;
    this.row = row;
  }

  /** Gets this named tile from a Fantasy Dreamland sheet. */
  public TextureRegion region(TileSheet tileSheet) {
    return tileSheet.tile(column, row);
  }
}
