package com.csse3200.game.components.rooms.configs;

/**
 * A piece of furniture or decoration placed in a themed room. Position is the tile the prop stands
 * on (its bottom-left corner).
 */
public class PropConfig extends PositionConfig {
  /** Name of a {@link com.csse3200.game.areas.attic.AtticProp}. */
  public String prop;

  /** Length in tiles, for stretchable props such as the reading table. 0 keeps the sprite size. */
  public int length;

  /**
   * Pixels to lift the sprite by, e.g. to stand a candle on a table top. Does not affect sorting.
   */
  public float elevation;
}
