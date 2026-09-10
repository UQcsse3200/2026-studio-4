package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.items.ItemType;

/** Temporary demo handler for using consumables stored in the player's inventory. */
public class ConsumableUseComponent extends Component {
  static final int HEALTH_POTION_HEAL = 25;

  private InventoryComponent inventory;
  private CombatStatsComponent combatStats;

  @Override
  public void create() {
    inventory = entity.getComponent(InventoryComponent.class);
    combatStats = entity.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("useConsumable", this::useConsumable);
  }

  private void useConsumable(ItemType itemType) {
    if (itemType != ItemType.HEALTH_POTION
        || combatStats.getHealth() >= combatStats.getMaxHealth()) {
      return;
    }

    if (inventory.removeConsumable(itemType)) {
      combatStats.addHealth(HEALTH_POTION_HEAL);
    }
  }
}
