package com.csse3200.game.components.rooms;

import com.csse3200.game.rooms.RoomSelector;
import com.csse3200.game.components.boss.FinalBossPhase;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.EntityService;
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

  private boolean scheduleRoomSwitch(String roomId) {
    WorldConfig world = roomManager.getWorld();
    EntityService entities = ServiceLocator.getEntityService();
    if (world == null || entities == null) return false;
    FinalBossPhase stage =
        switch (roomId) {
          case "finalBossStage1" -> FinalBossPhase.STAGE_ONE;
          case "finalBossStage2" -> FinalBossPhase.STAGE_TWO;
          case "finalBossStage3" -> FinalBossPhase.STAGE_THREE;
          default -> null;
        };
    RoomConfig room = world.getRoom(stage == null ? roomId : "finalBossRoom");
    if (room == null || (stage != null && !RoomManager.hasFinalBoss(room))) return false;

    // Validate before scheduling: malformed commands must not replace a pending room switch.
    entities.schedule(
        () -> {
          if (stage == null) roomManager.debugSwitchRoom(room);
          else roomManager.debugSwitchFinalBossRoom(room, stage);
        });
    return true;
  }
}
