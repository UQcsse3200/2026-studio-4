package com.csse3200.game.components.rooms;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.rooms.configs.RoomConfig;

/** Provides access to the Config that the room entity is build on */
public class RoomDataComponent extends Component {

  private RoomConfig roomConfig;

  public RoomDataComponent(RoomConfig roomConfig) {
    this.roomConfig = roomConfig;
  }

  public RoomConfig getConfig() {
    return roomConfig;
  }
}
