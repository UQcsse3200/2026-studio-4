package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemIds;
import java.util.ArrayList;
import java.util.Map;

/** Grants consumables through the existing inventory: {@code con <item> [quantity]}. */
public class ConsumableCommand implements Command {
  private static final int MAX_QUANTITY = 25;
  private static final Map<String, String> ITEMS =
      Map.of(
          "small", ItemIds.HEALTH_POTION,
          "medium", ItemIds.MEDIUM_HEALTH_POTION,
          "large", ItemIds.LARGE_HEALTH_POTION,
          "shield", ItemIds.SHIELD,
          "speed", ItemIds.SPEED_POTION,
          "strength", ItemIds.STRENGTH_POTION,
          "freeze", ItemIds.FREEZE_BOMB,
          "burn", ItemIds.BURN_VIAL,
          "magnet", ItemIds.MAGNET_POTION);

  private final Entity player;

  public ConsumableCommand(Entity player) {
    this.player = player;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.isEmpty()) {
      return false;
    }
    // Also accept "con heal small", matching the healing-tier names used during QA.
    int itemIndex = "heal".equals(args.getFirst()) ? 1 : 0;
    if (args.size() <= itemIndex || args.size() > itemIndex + 2) {
      return false;
    }
    String name = args.get(itemIndex);
    if (itemIndex == 1
        && !"small".equals(name)
        && !"medium".equals(name)
        && !"large".equals(name)) {
      return false;
    }
    String itemId = ITEMS.get(name);
    if (itemId == null) {
      return false;
    }
    int quantity = 1;
    if (args.size() == itemIndex + 2) {
      String value = args.get(itemIndex + 1);
      if (!value.matches("\\d+")) {
        return false;
      }
      quantity = 0;
      // Clamp while parsing, so even numbers larger than Integer.MAX_VALUE are safe.
      for (int i = 0; i < value.length(); i++) {
        quantity = Math.min(MAX_QUANTITY, quantity * 10 + value.charAt(i) - '0');
      }
      if (quantity == 0) {
        return false;
      }
    }
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    if (inventory == null) {
      return false;
    }
    inventory.addConsumable(itemId, quantity);
    return true;
  }
}
