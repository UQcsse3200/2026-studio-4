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
 * <p>Burn Vial has no hotbar slot or loot-table entry yet (both are pending team decisions), so
 * there is no other way to trigger it in a running game. This lets QA see the on-screen-enemy burn
 * effect without either: {@code burnvial}.
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
   * Grants and consumes one Burn Vial.
   *
   * @param args no arguments expected
   * @return true if the Burn Vial was granted and the use request was sent
   */
  @Override
  public boolean action(ArrayList<String> args) {
    if (!args.isEmpty()) {
      logger.debug("Invalid arguments received for 'burnvial' command: {}", args);
      return false;
    }

    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    if (inventory == null) {
      logger.debug("Player has no InventoryComponent; cannot grant a Burn Vial");
      return false;
    }

    inventory.addConsumable(ItemIds.BURN_VIAL, 1);
    player.getEvents().trigger(ConsumableEffectComponent.USE_REQUEST, ItemIds.BURN_VIAL);
    return true;
  }
}
