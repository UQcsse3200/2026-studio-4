package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.entities.Entity;
import java.util.ArrayList;

/**
 * Compatibility alias for {@code con magnet [quantity]}. No arguments grants one potion; quantities
 * use the shared per-command cap of 25 without consuming the granted stock.
 */
public class MagnetCommand implements Command {
  private final ConsumableCommand grant;

  public MagnetCommand(Entity player) {
    grant = new ConsumableCommand(player);
  }

  @Override
  public boolean action(ArrayList<String> args) {
    ArrayList<String> request = new ArrayList<>();
    request.add("magnet");
    request.addAll(args);
    return grant.action(request);
  }
}
