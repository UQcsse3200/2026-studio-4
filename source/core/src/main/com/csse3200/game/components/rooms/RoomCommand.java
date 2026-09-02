package com.csse3200.game.components.rooms;

import com.csse3200.game.ui.terminal.commands.Command;
import java.util.ArrayList;

public class RoomCommand implements Command {
  private final RoomManager roomManager;

  public RoomCommand(RoomManager roomManager) {
    this.roomManager = roomManager;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1 || !"clear".equals(args.get(0))) {
      return false;
    }
    roomManager.clearCurrentRoom();
    return true;
  }
}
