package com.csse3200.game.saving;

public final class RunSaveState {
  public static String currentRoomId;

  private RunSaveState() {}

  public static void setCurrentRoomId(String roomId) {
    currentRoomId = roomId;
  }

  public static String getCurrentRoomId() {
    if (currentRoomId == null) {
      throw new IllegalStateException("Current room ID has not been set.");
    }
    return currentRoomId;
  }

  public static void reset() {
    currentRoomId = null;
  }
}
