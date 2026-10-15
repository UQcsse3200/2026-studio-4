package com.csse3200.game.components.rooms;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.rooms.RoomClearService;
import java.util.Objects;

/** Provides access to the Config that the room entity is build on */
public class RoomDataComponent extends Component {

  private RoomConfig roomConfig;
  private RoomClearService roomClearService;

  public RoomDataComponent(RoomConfig roomConfig, RoomClearService roomClearService) {
    this.roomConfig = Objects.requireNonNull(roomConfig);
    this.roomClearService = Objects.requireNonNull(roomClearService);
  }

  public RoomClearService getRoomClear() {
    return roomClearService;
  }

  public RoomConfig getConfig() {
    return roomConfig;
  }
}
