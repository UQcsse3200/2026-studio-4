package com.csse3200.game.components.rooms;

import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.commands.Command;
import java.util.ArrayList;

public class RoomCommand implements Command {
  private final RoomManager roomManager;

  public RoomCommand(RoomManager roomManager) {
    this.roomManager = roomManager;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.isEmpty()) return false;

    switch (args.get(0)) {
      case "clear":
        roomManager.clearCurrentRoom();
        return true;
      case "goto":
        if (args.size() < 2) return false;

        RoomConfig room = roomManager.getWorld().getRoom(args.get(1));
        if (room == null) return false;

        // uses first avaliable exit in room as spawn since all rooms must have exits
        ServiceLocator.getEntityService().schedule(() -> roomManager.debugSwitchRoom(room));
        return true;
      default:
        return false;
    }
  }
}
