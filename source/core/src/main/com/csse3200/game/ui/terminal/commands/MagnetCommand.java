package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemIds;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terminal command that gives the player Magnet Potions so QA can test them without waiting for a
 * drop: {@code magnet} gives one, {@code magnet 3} gives three.
 */
public class MagnetCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(MagnetCommand.class);
  private final Entity player;

  /**
   * @param player entity carrying {@link InventoryComponent}
   */
  public MagnetCommand(Entity player) {
    this.player = player;
  }

  /**
   * Adds Magnet Potions to the player's inventory.
   *
   * @param args empty for one potion, or a single positive amount
   * @return true if potions were added
   */
  @Override
  public boolean action(ArrayList<String> args) {
    int amount = 1;
    if (args.size() == 1) {
      try {
        amount = Integer.parseInt(args.get(0));
      } catch (NumberFormatException e) {
        amount = 0;
      }
    }
    if (args.size() > 1 || amount <= 0) {
      logger.debug("Invalid arguments received for 'magnet' command: {}", args);
      return false;
    }
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    if (inventory == null) {
      logger.debug("Player has no InventoryComponent; cannot give Magnet Potions");
      return false;
    }
    inventory.addConsumable(ItemIds.MAGNET_POTION, amount);
    return true;
  }
}
