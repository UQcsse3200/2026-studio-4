package com.csse3200.game.rooms;

import com.csse3200.game.services.ServiceLocator;
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
      case "unlock_final":
        ServiceLocator.getRoomClearService().clearAllDungeons();
        return true;
      case "goto":
        return true;
      default:
        return false;
    }
  }
}
