package com.csse3200.game.areas.attic;

/** Texture paths for the attic art used by the dungeon selection room. */
public final class AtticSprites {
  /** Width and height of one tile on every attic sheet, in pixels. */
  public static final int TILE_PIXELS = 16;

  /** Pixels per world unit. A terrain tile is 0.5 world units, so 16px tiles draw at 32px/unit. */
  public static final float PIXELS_PER_UNIT = 32f;

  public static final String FURNITURE_WOOD = "images/attic/furniture_wood.png";
  public static final String FURNITURE_GREEN = "images/attic/furniture_green.png";
  public static final String ITEMS = "images/attic/items.png";
  public static final String FLOORS = "images/attic/floors.png";
  public static final String WALLS = "images/attic/walls.png";
  public static final String TOMES = "images/attic/tomes.png";
  public static final String DOORS_WINDOWS = "images/attic/doors_windows.png";

  /**
   * The default dungeon tileset (brown wood and green stone). Attic props such as dusty shelves,
   * candle stands, crates and lanterns are cut from it. It is loaded with the other dungeon
   * tilesets by {@code RoomAssets}, so it is not part of {@link #paths()}.
   */
  public static final String ATTIC_TILESET = "images/dungeons/tileSet0.png";

  private static final String[] ALL = {
    FURNITURE_WOOD, FURNITURE_GREEN, ITEMS, FLOORS, WALLS, TOMES, DOORS_WINDOWS
  };

  private AtticSprites() {
    throw new IllegalStateException("Instantiating static util class");
  }

  /** Every texture a attic room needs, for loading through the resource service. */
  public static String[] paths() {
    return ALL.clone();
  }

  /** Converts a length in sprite pixels to world units. */
  public static float toWorld(float pixels) {
    return pixels / PIXELS_PER_UNIT;
  }
}
