package com.csse3200.game.components.rooms.configs;

import java.util.Objects;

/** The complete declarative room graph loaded from {@code configs/rooms.json}. */
public class WorldConfig {
  public String startRoomId;
  public String startEntryPointId;
  public RoomConfig[] rooms = new RoomConfig[0];

  /** Checks the graph references required while transitioning between rooms. */
  public void validate() {
    RoomConfig startRoom = getRoom(startRoomId);
    require(startRoom != null, "Unknown start room: " + startRoomId);
    require(
        startRoom.getEntryPoint(startEntryPointId) != null,
        "Unknown start entry point: " + startEntryPointId);

    for (RoomConfig room : rooms) {
      for (ExitConfig exit : room.exits) {
        validateExit(room, exit);
      }
    }
  }

  /** Gets a room by its stable id, or null when it is not defined. */
  public RoomConfig getRoom(String roomId) {
    for (RoomConfig room : rooms) {
      if (Objects.equals(room.id, roomId)) {
        return room;
      }
    }
    return null;
  }

  private void validateExit(RoomConfig room, ExitConfig exit) {
    if (!exit.available) {
      return;
    }
    RoomConfig destination = getRoom(exit.destinationRoomId);
    require(destination != null, "Unknown destination room: " + exit.destinationRoomId);
    if ("DOOR".equals(exit.kind)) {
      validateDoorPair(room, exit, destination);
      return;
    }
    require("BOOKSHELF".equals(exit.kind), "Unknown exit kind: " + exit.kind);
    require(
        destination.getEntryPoint(exit.destinationEntryPointId) != null,
        "Unknown destination entry point: " + exit.destinationEntryPointId);
  }

  private static void validateDoorPair(RoomConfig room, ExitConfig exit, RoomConfig destination) {
    ExitConfig paired = destination.getExit(exit.destinationExitId);
    require(paired != null, "Unknown paired door: " + exit.destinationExitId);
    require(
        "DOOR".equals(paired.kind)
            && paired.available
            && Objects.equals(room.id, paired.destinationRoomId)
            && Objects.equals(exit.id, paired.destinationExitId),
        "Door pair is not reciprocal: " + exit.id);
    require(opposite(exit.side).equals(paired.side), "Door pair faces the wrong way: " + exit.id);
  }

  private static String opposite(String side) {
    if ("LEFT".equals(side)) return "RIGHT";
    if ("RIGHT".equals(side)) return "LEFT";
    if ("TOP".equals(side)) return "BOTTOM";
    if ("BOTTOM".equals(side)) return "TOP";
    return "";
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }
}
