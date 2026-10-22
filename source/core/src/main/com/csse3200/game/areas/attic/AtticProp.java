package com.csse3200.game.areas.attic;

import com.badlogic.gdx.graphics.Color;

/**
 * Furniture and ornaments that can be placed in an attic room, keyed by the names used in {@code
 * rooms.json} ({@code "prop"} on a room prop, {@code "sprite"} on an exit).
 *
 * <p>Each entry is a pixel rectangle on one of the {@link AtticSprites} sheets plus a collision
 * footprint. Sprites are drawn at their native size (16px = one terrain tile), so a 48x48 bookshelf
 * covers 3x3 tiles. The footprint is a box centred along the bottom of the sprite: furniture is
 * drawn from a three-quarter view, so only its base should block the player.
 */
public enum AtticProp {
  BOOKSHELF_TALL(AtticSprites.FURNITURE_WOOD, 32, 64, 48, 48, 1f, 20),
  BOOKSHELF_DARK(AtticSprites.FURNITURE_WOOD, 96, 64, 48, 48, 1f, 20),
  BOOKSHELF_NARROW(AtticSprites.FURNITURE_WOOD, 80, 64, 16, 48, 1f, 20),
  BOOKSHELF_NARROW_DARK(AtticSprites.FURNITURE_WOOD, 144, 64, 16, 48, 1f, 20),
  BOOKSHELF_LOW(AtticSprites.FURNITURE_WOOD, 48, 32, 32, 32, 1f, 16),
  WOOD_STOVE(AtticSprites.FURNITURE_WOOD, 144, 112, 32, 48, 0.8f, 16),
  GRANDFATHER_CLOCK(AtticSprites.FURNITURE_WOOD, 64, 112, 32, 48, 0.6f, 12),
  FLOOR_LAMP(AtticSprites.FURNITURE_WOOD, 96, 112, 16, 48, 0.5f, 6),
  POTTED_PLANT(AtticSprites.FURNITURE_WOOD, 0, 160, 16, 32, 0.6f, 8),
  /** Three-slice table: set {@code "length"} (tiles) on the prop to stretch it. */
  READING_TABLE(
      new Region(AtticSprites.FURNITURE_WOOD, 0, 32, 48, 32), 1f, 28, Traits.stretchable(16)),
  DESK(AtticSprites.FURNITURE_WOOD, 0, 64, 32, 48, 1f, 40),
  PEDESTAL_TABLE(AtticSprites.FURNITURE_WOOD, 176, 0, 16, 32, 0.7f, 10),
  CHAIR(AtticSprites.FURNITURE_WOOD, 128, 0, 16, 32, 0.8f, 8),
  COFFEE_TABLE(AtticSprites.FURNITURE_WOOD, 80, 40, 32, 24, 0.9f, 12),
  SOFA_GREEN(AtticSprites.FURNITURE_GREEN, 16, 160, 64, 32, 0.9f, 16),
  ARMCHAIR_GREEN(AtticSprites.FURNITURE_GREEN, 144, 160, 32, 32, 0.9f, 14),
  ARMCHAIR_SIDE_GREEN(AtticSprites.FURNITURE_GREEN, 176, 112, 32, 48, 0.9f, 20),
  MIRROR(AtticSprites.FURNITURE_WOOD, 112, 112, 32, 48, 0.8f, 10),
  BOX(AtticSprites.ITEMS, 96, 32, 16, 16, 0.8f, 8),
  CRATE(AtticSprites.ITEMS, 112, 32, 16, 16, 0.9f, 10),
  /** Top-down staircase with a railing, leading down out of the attic. Blocks its whole area. */
  STAIRS_DOWN(AtticSprites.DOORS_WINDOWS, 0, 96, 32, 64, 1f, 60),
  /* Attic pieces from the dungeon tileset. */
  DUSTY_BOOKSHELF(AtticSprites.ATTIC_TILESET, 0, 288, 32, 32, 1f, 14),
  DUSTY_BOOKSHELF_HALF(AtticSprites.ATTIC_TILESET, 32, 288, 32, 32, 1f, 14),
  DUSTY_SHELF_EMPTY(AtticSprites.ATTIC_TILESET, 64, 288, 32, 32, 1f, 14),
  DUSTY_BOOKSHELF_NARROW(AtticSprites.ATTIC_TILESET, 96, 288, 16, 32, 1f, 14),
  CANDLE_STAND(AtticSprites.ATTIC_TILESET, 160, 256, 16, 32, 0.6f, 8),
  CRATE_PILE(AtticSprites.ATTIC_TILESET, 0, 320, 32, 32, 0.9f, 18),
  BARREL_PILE(AtticSprites.ATTIC_TILESET, 64, 320, 32, 32, 0.9f, 18),
  CRATE_STACK(AtticSprites.ATTIC_TILESET, 128, 320, 32, 32, 0.9f, 18),
  OLD_TABLE(AtticSprites.ATTIC_TILESET, 192, 320, 32, 32, 1f, 20),
  /** Place on the row in front of a wall with ~24px elevation to hang it on the wall face. */
  HANGING_LANTERN(AtticSprites.ATTIC_TILESET, 336, 80, 16, 16, 0f, 0),

  /** Small window for the back wall; place it on the wall row with a little elevation. */
  DORMER_WINDOW(AtticSprites.DOORS_WINDOWS, 160, 64, 32, 16, 0f, 0),

  RUG(AtticSprites.FURNITURE_WOOD, 160, 32, 48, 32),
  RUG_TALL(AtticSprites.FURNITURE_WOOD, 168, 64, 32, 48),
  RUG_OVAL(AtticSprites.FURNITURE_WOOD, 128, 48, 32, 16),

  CANDLE(AtticSprites.ITEMS, 32, 48, 16, 16, 0f, 0),
  OPEN_BOOK(AtticSprites.ITEMS, 0, 48, 16, 16, 0f, 0),
  BOOK_STACK(AtticSprites.ITEMS, 64, 16, 16, 16, 0f, 0),
  CRYSTAL_BALL(AtticSprites.ITEMS, 64, 48, 16, 16, 0f, 0),
  QUILL_POT(AtticSprites.ITEMS, 48, 0, 16, 16, 0f, 0),
  TEAPOT(AtticSprites.ITEMS, 112, 112, 16, 16, 0f, 0),
  TEACUP(AtticSprites.ITEMS, 64, 96, 16, 16, 0f, 0),

  /** Dungeon tomes: the books on the reading table that act as dungeon entrances. */
  TOME_TEMPLE(0, new Color(0.45f, 0.85f, 0.35f, 1f)),
  TOME_DESERT(1, new Color(1f, 0.78f, 0.35f, 1f)),
  TOME_FROST(2, new Color(0.5f, 0.8f, 1f, 1f)),
  TOME_DRAGON(3, new Color(1f, 0.35f, 0.3f, 1f)),
  TOME_FINAL(4, new Color(0.7f, 0.45f, 1f, 1f));

  /** Where a prop's sprite is: a pixel rectangle on one of the {@link AtticSprites} sheets. */
  record Region(String sheet, int x, int y, int width, int height) {}

  /** The less common behaviours a prop can have. Ordinary furniture has {@link #NONE}. */
  record Traits(int sliceWidth, boolean hasFloor, Color glow) {
    static final Traits NONE = new Traits(0, false, null);
    static final Traits FLOOR = new Traits(0, true, null);

    /** Stretches horizontally: left cap, repeated middle, right cap, each this many pixels. */
    static Traits stretchable(int sliceWidth) {
      return new Traits(sliceWidth, false, null);
    }

    static Traits glowing(Color glow) {
      return new Traits(0, false, glow);
    }
  }

  private final Region region;
  private final float colliderWidth;
  private final int colliderHeight;
  private final Traits traits;

  /** Solid furniture. */
  AtticProp(
      String sheet, int x, int y, int width, int height, float colliderWidth, int colliderHeight) {
    this(new Region(sheet, x, y, width, height), colliderWidth, colliderHeight, Traits.NONE);
  }

  /** Floor covering (rugs): no collider, always drawn beneath everything standing on it. */
  AtticProp(String sheet, int x, int y, int width, int height) {
    this(new Region(sheet, x, y, width, height), 0f, 0, Traits.FLOOR);
  }

  /** A dungeon tome from the generated tome strip, with the colour of its magical glow. */
  AtticProp(int tomeIndex, Color glow) {
    this(
        new Region(
            AtticSprites.TOMES,
            tomeIndex * AtticSprites.TILE_PIXELS,
            0,
            AtticSprites.TILE_PIXELS,
            AtticSprites.TILE_PIXELS),
        0f,
        0,
        Traits.glowing(glow));
  }

  AtticProp(Region region, float colliderWidth, int colliderHeight, Traits traits) {
    this.region = region;
    this.colliderWidth = colliderWidth;
    this.colliderHeight = colliderHeight;
    this.traits = traits;
  }

  /**
   * Looks a prop up by its rooms.json name.
   *
   * @throws IllegalArgumentException naming the bad value, if there is no such prop
   */
  public static AtticProp fromName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Attic prop name is missing");
    }
    try {
      return valueOf(name);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown attic prop: " + name, e);
    }
  }

  public String getSheet() {
    return region.sheet();
  }

  public int getX() {
    return region.x();
  }

  public int getY() {
    return region.y();
  }

  public int getWidth() {
    return region.width();
  }

  public int getHeight() {
    return region.height();
  }

  /** Collider width as a fraction of the drawn width; 0 when the prop has no collider. */
  public float getColliderWidth() {
    return colliderWidth;
  }

  /** Collider height in sprite pixels, measured up from the bottom of the sprite. */
  public int getColliderHeight() {
    return colliderHeight;
  }

  public boolean isSolid() {
    return colliderWidth > 0f && colliderHeight > 0;
  }

  /** Width of the end caps for stretchable props, or 0 if the prop does not stretch. */
  public int getSliceWidth() {
    return traits.sliceWidth();
  }

  public boolean isStretchable() {
    return traits.sliceWidth() > 0;
  }

  /** Whether the prop lies flat on the floor (drawn under all standing entities). */
  public boolean isFloorCovering() {
    return traits.hasFloor();
  }

  /** Glow colour for magical props, or null for ordinary furniture. */
  public Color getGlow() {
    return traits.glow() == null ? null : traits.glow().cpy();
  }

  /**
   * Drawn width in pixels for a prop placed with the given length.
   *
   * @param lengthTiles requested length in tiles; ignored (and may be 0) for fixed-size props
   */
  public int drawnWidth(int lengthTiles) {
    if (!isStretchable() || lengthTiles <= 0) {
      return region.width();
    }
    return Math.max(2, lengthTiles) * AtticSprites.TILE_PIXELS;
  }
}
