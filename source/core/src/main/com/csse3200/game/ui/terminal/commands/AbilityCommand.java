package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;

/** Normal gameplay entry point: {@code ability invisibility|laststand}, with no cooldown bypass. */
public class AbilityCommand implements Command {
  private final Entity player;

  /** Binds the command to the player carrying the abilities component. */
  public AbilityCommand(Entity player) {
    this.player = player;
  }

  /** Casts invisibility or idempotently enables Last Stand; never directly triggers the passive. */
  @Override
  public boolean action(ArrayList<String> args) {
    if (player == null || args == null || args.size() != 1) {
      return false;
    }
    PlayerAbilitiesComponent abilities = player.getComponent(PlayerAbilitiesComponent.class);
    if (abilities == null) {
      return false;
    }
    switch (args.get(0)) {
      case "invisibility":
        return abilities.tryInvisibility();
      case "laststand":
        abilities.enableLastStand();
        return true;
      default:
        return false;
    }
  }
}
