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
}
