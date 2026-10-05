package com.csse3200.game.areas.terrain;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.*;

/** Curated tile palette for the prototype Fantasy Dreamland dungeon. */
public enum DreamlandTile {
  VOID(' ', 4, 6),
  EARTH('1', 1, 0),
  EARTH_PATH1('2', 2, 1),
  EARTH_PATH2('2', 3, 1),
  EARTH_TILE_1('3', 2, 0),
  EARTH_TILE_2('3', 3, 0),
  EARTH_TILE_3('3', 2, 2),
  EARTH_TILE_4('3', 3, 2),
  TILE_TYPE1_1('4', 4, 0),
  TILE_TYPE1_2('4', 7, 0),
  TILE_TYPE1_3('4', 8, 0),
  TILE_TYPE2_1('5', 4, 1),
  TILE_TYPE2_2('5', 7, 1),
  TILE_TYPE2_3('5', 8, 1),
  TILE_TYPE3_1('6', 4, 2),
  TILE_TYPE3_2('6', 7, 2),
  TILE_TYPE3_3('6', 8, 2),
  FLOOR_TYPE1_1('7', 9, 0),
  FLOOR_TYPE1_2('7', 10, 0),
  FLOOR_TYPE1_3('7', 14, 0),
  FLOOR_TYPE2_1('8', 9, 1),
  FLOOR_TYPE2_2('8', 10, 1),
  FLOOR_TYPE2_3('8', 14, 1),

  WALL_TYPE1_UP('o', 4, 8, 2, 1),
  WALL_TYPE1_LEFT('o', 0, 6),
  WALL_TYPE1_RIGHT('o', 2, 6),
  WALL_TYPE1_DOWN('o', 1, 8),
  WALL_TYPE1_UP_RIGHT('o', 3, 8, 2, 1),
  WALL_TYPE1_UP_LEFT('o', 5, 8, 2, 1),
  WALL_TYPE1_DOWN_RIGHT('o', 3, 5),
  WALL_TYPE1_DOWN_LEFT('o', 5, 5),

  CHAIN('P', 20, 6),
  PURPLE_STONE_FLOOR('b', 4, 0),
  PURPLE_STONE_WALL('c', 8, 0),
  BLUE_STONE_WALL('d', 9, 0),
  BOOKSHELF('e', 0, 16, 2, 2),
  OPEN_BARREL('h', 6, 23);

  public static final DreamlandTile FLOOR_STONE = PURPLE_STONE_FLOOR;
  public static final DreamlandTile WALL_STONE = PURPLE_STONE_WALL;

  private final int column;
  private final int row;

  private final int height;

  private final int width;

  private final Character id;

  DreamlandTile(Character id, int column, int row) {
    this(id, column, row, 1, 1);
  }

  DreamlandTile(Character id, int column, int row, int height, int width) {
    this.id = id;
    this.column = column;
    this.row = row;
    this.height = height;
    this.width = width;
  }

  private static final Map<Character, List<DreamlandTile>> BY_ID = new HashMap<>();

  static {
    for (DreamlandTile t : values()) {
      BY_ID.computeIfAbsent(t.id, k -> new ArrayList<>()).add(t);
    }
  }

  public int getId() {
    return id;
  }

  /** Every tile with this id, or an empty list if there are none. */
  public static List<DreamlandTile> allWithId(Character id) {
    return Collections.unmodifiableList(BY_ID.getOrDefault(id, List.of()));
  }

  /** Gets this named tile from a Fantasy Dreamland sheet. */
  public TextureRegion region(TileSheet tileSheet) {
    return this.region(tileSheet, 0, 0);
  }

  public TextureRegion region(TileSheet tileSheet, int x, int y) {
    return tileSheet.tile(column + x, row + y, height, width);
  }
}
