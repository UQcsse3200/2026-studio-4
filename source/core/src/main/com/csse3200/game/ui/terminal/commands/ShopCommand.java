package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.maingame.ShopDisplay;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terminal command that opens or closes the shop display so QA does not need a Merchant NPC:
 * {@code shop open} or {@code shop close}.
 *
 * <p>Standing in for the real Merchant NPC interaction (Sprint 3 #202, owned by Team 3), which is
 * not yet available. Once that lands, the NPC's interaction event should call {@link
 * ShopDisplay#open()} the same way this command does.
 */
public class ShopCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(ShopCommand.class);
  private static final String OPEN = "open";
  private static final String CLOSE = "close";

  private final Entity ui;

  /**
   * @param ui entity carrying the {@link ShopDisplay}
   */
  public ShopCommand(Entity ui) {
    this.ui = ui;
  }

  /**
   * Opens or closes the shop display.
   *
   * @param args a single argument, either {@code open} or {@code close}
   * @return true if the shop's visibility was changed
   */
  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1 || !(OPEN.equals(args.get(0)) || CLOSE.equals(args.get(0)))) {
      logger.debug("Invalid arguments received for 'shop' command: {}", args);
      return false;
    }

    ShopDisplay display = ui.getComponent(ShopDisplay.class);
    if (display == null) {
      logger.debug("UI entity has no ShopDisplay; cannot toggle shop");
      return false;
    }

    if (OPEN.equals(args.get(0))) {
      display.open();
    } else {
      display.close();
    }
    return true;
  }
}
