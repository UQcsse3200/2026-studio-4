package com.csse3200.game.components.rooms;

import com.csse3200.game.rooms.RoomSelector;
import com.csse3200.game.ui.terminal.commands.Command;
import java.util.ArrayList;

public class RoomCommand implements Command {
  private final RoomSelector roomSelector;

  public RoomCommand(RoomSelector roomSelector) {
    this.roomSelector = roomSelector;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.isEmpty()) return false;

    switch (args.get(0)) {
      case "clear":
        roomSelector.clearRoom();
        return true;
      case "goto":
        // if (args.size() < 2) return false;

        // RoomConfig room = roomSelector.getWorld().getRoom(args.get(1));
        // if (room == null) return false;

        // // uses first avaliable exit in room as spawn since all rooms must have exits
        // ServiceLocator.getEntityService().schedule(() -> roomSelector.debugSwitchRoom(room));
        return true;
      default:
        return false;
    }
  }
}
