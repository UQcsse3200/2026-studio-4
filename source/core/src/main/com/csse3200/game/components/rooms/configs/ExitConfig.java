package com.csse3200.game.components.rooms.configs;

/** A visible, interactable route from one room to another. */
public class ExitConfig extends PositionConfig {
  public String id;
  public String kind;
  public String destinationRoomId;
  public String destinationEntryPointId;
  public String destinationExitId;
  public String side;
  public boolean available = true;
  public boolean requiresClear;
  public boolean completesDungeon;
  public String message;

  /**
   * Optional {@link com.csse3200.game.areas.attic.AtticProp} to draw instead of the default door or
   * bookshelf, e.g. a dungeon tome lying on the attic table.
   */
  public String sprite;

  /** Pixels to lift {@link #sprite} by (e.g. onto a table top). Interaction uses x/y unchanged. */
  public float elevation;
}
