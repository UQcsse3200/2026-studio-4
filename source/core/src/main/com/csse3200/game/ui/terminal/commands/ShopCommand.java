package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.shop.ShopSessionComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terminal command that opens or closes the shop session so QA does not need a Merchant NPC: {@code
 * shop open} or {@code shop close}.
 *
 * <p>Adapted from Aarash Mehta's shop display command. Both this debug entry and the Merchant
 * interaction use the same session, including world freeze, control locks and focus restoration.
 */
public class ShopCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(ShopCommand.class);
  private static final String OPEN = "open";
  private static final String CLOSE = "close";

  private final Entity ui;

  /**
   * @param ui entity carrying the {@link ShopSessionComponent}
   */
  public ShopCommand(Entity ui) {
    this.ui = ui;
  }

  /**
   * Opens or closes the shop session.
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

    ShopSessionComponent display = ui.getComponent(ShopSessionComponent.class);
    if (display == null) {
      logger.debug("Shop entity has no ShopSessionComponent; cannot toggle shop");
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
