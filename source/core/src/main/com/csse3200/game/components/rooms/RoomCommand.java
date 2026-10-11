package com.csse3200.game.components.rooms;

import com.csse3200.game.components.boss.FinalBossPhase;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.EntityService;
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
    if (roomManager == null || args == null || args.isEmpty() || args.contains(null)) return false;
    if ("clear".equals(args.get(0))) {
      if (args.size() != 1) return false;
      roomManager.clearCurrentRoom();
      return true;
    }
    if (!"goto".equals(args.get(0)) || args.size() != 2 || args.get(1).isBlank()) return false;

    WorldConfig world = roomManager.getWorld();
    EntityService entities = ServiceLocator.getEntityService();
    if (world == null || entities == null) return false;
    FinalBossPhase stage =
        switch (args.get(1)) {
          case "finalBossStage1" -> FinalBossPhase.STAGE_ONE;
          case "finalBossStage2" -> FinalBossPhase.STAGE_TWO;
          case "finalBossStage3" -> FinalBossPhase.STAGE_THREE;
          default -> null;
        };
    RoomConfig room = world.getRoom(stage == null ? args.get(1) : "finalBossRoom");
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
