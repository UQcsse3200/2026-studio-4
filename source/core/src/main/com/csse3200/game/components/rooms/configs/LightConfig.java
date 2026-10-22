package com.csse3200.game.components.rooms.configs;

/**
 * A free-standing light in a dark room, such as moonlight through a window. Centred on its tile.
 */
public class LightConfig extends PositionConfig {
  /** Reach of the light in world units (a tile is 0.5). */
  public float radius = 2f;

  /** Light colour as hex, e.g. {@code "#9fb8ff"}. */
  public String color = "#ffffff";

  /** 0 for a steady light, up to 1 for a lively flame. */
  public float flicker;
}
