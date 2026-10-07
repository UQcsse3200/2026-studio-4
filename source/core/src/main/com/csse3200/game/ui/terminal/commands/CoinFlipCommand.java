package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.maingame.CoinFlipDisplay;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terminal command that opens or closes the coin flip booth so QA does not need a Casino NPC:
 * {@code coinflip open} or {@code coinflip close}.
 *
 * <p>Standing in for a real Casino/Gambler NPC interaction, which does not exist yet. Once one
 * lands, the NPC's interaction event should call {@link CoinFlipDisplay#open()} the same way this
 * command does.
 */
public class CoinFlipCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(CoinFlipCommand.class);
  private static final String OPEN = "open";
  private static final String CLOSE = "close";

  private final Entity ui;

  /**
   * @param ui entity carrying the {@link CoinFlipDisplay}
   */
  public CoinFlipCommand(Entity ui) {
    this.ui = ui;
  }

  /**
   * Opens or closes the coin flip booth.
   *
   * @param args a single argument, either {@code open} or {@code close}
   * @return true if the booth's visibility was changed
   */
  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1 || !(OPEN.equals(args.get(0)) || CLOSE.equals(args.get(0)))) {
      logger.debug("Invalid arguments received for 'coinflip' command: {}", args);
      return false;
    }

    CoinFlipDisplay display = ui.getComponent(CoinFlipDisplay.class);
    if (display == null) {
      logger.debug("UI entity has no CoinFlipDisplay; cannot toggle coin flip booth");
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
