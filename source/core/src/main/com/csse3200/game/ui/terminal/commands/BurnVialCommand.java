package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemIds;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terminal command that grants the player one Burn Vial and immediately consumes it.
 *
 * <p>{@code burnvial} retains the original immediate-use QA path. {@code burnvial give} grants one
 * vial without using it, so QA can exercise the existing four-slot inventory and Tab/Q input.
 */
public class BurnVialCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(BurnVialCommand.class);

  private final Entity player;

  /**
   * @param player entity carrying {@link InventoryComponent} and {@link ConsumableEffectComponent}
   */
  public BurnVialCommand(Entity player) {
    this.player = player;
  }

  /**
   * Grants one Burn Vial, optionally leaving it in inventory for normal selection and use.
   *
   * @param args empty for immediate use, or {@code give} for pickup-only testing
   * @return true if the vial was granted; immediate mode also sends a use request, whose success
   *     depends on the normal consumable-use validation
   */
  @Override
  public boolean action(ArrayList<String> args) {
    boolean giveOnly = args.size() == 1 && "give".equals(args.get(0));
    if (!args.isEmpty() && !giveOnly) {
      logger.debug("Invalid arguments received for 'burnvial' command: {}", args);
      return false;
    }

    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    if (inventory == null) {
      logger.debug("Player has no InventoryComponent; cannot grant a Burn Vial");
      return false;
    }

    inventory.addConsumable(ItemIds.BURN_VIAL, 1);
    if (!giveOnly) {
      player.getEvents().trigger(ConsumableEffectComponent.USE_REQUEST, ItemIds.BURN_VIAL);
    }
    return true;
  }
}
