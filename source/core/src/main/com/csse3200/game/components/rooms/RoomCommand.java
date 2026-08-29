package com.csse3200.game.components.rooms;

import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.ui.terminal.commands.Command;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(RoomCommand.class);
  private RoomManager roomManager;
  private CameraComponent camera;

  public RoomCommand(RoomManager roomManager, CameraComponent camera) {
    this.roomManager = roomManager;
    this.camera = camera;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    switch (args.get(0)) {
      case "new":
        if (args.size() != 2) return false;
        roomManager.switchRoom(RoomFactory.createRoom(args.get(1), camera));
        return true;
      default:
        logger.debug("Bad command args");
        return false;
    }
  }
}
