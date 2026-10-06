package com.csse3200.game.components.maingame;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.charms.Charm;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryActions extends Component {
  private static final Logger logger = LoggerFactory.getLogger(InventoryActions.class);
  private final InventoryDisplay inventoryDisplay;

  public InventoryActions(InventoryDisplay inventoryDisplay) {
    this.inventoryDisplay = inventoryDisplay;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("nextPage", this::nextPage);
    entity.getEvents().addListener("moveActiveToInactiveItem", this::moveActiveToInactiveItem);
    entity.getEvents().addListener("moveInactiveToActiveItem", this::moveInactiveToActiveItem);
    entity.getEvents().addListener("moveActiveItem", this::moveActiveItem);
  }

  /** Changes page from the current inventory page to the non-displayed page */
  private void nextPage() {
    logger.info("Swap inventory page");
    inventoryDisplay.changePage();
  }

  private void moveActiveToInactiveItem(int fromIndex, int toIndex) {
    setEquipped(true, fromIndex, false, toIndex);
  }

  private void moveInactiveToActiveItem(int fromIndex, int toIndex) {
    setEquipped(false, fromIndex, true, toIndex);
  }

  private void moveActiveItem(int fromIndex, int toIndex) {
    setEquipped(true, fromIndex, true, toIndex);
  }

  private void setEquipped(boolean fromEquipped, int fromIndex, boolean equipped, int toIndex) {
    List<? extends Item> items = inventoryDisplay.getPageItems(fromEquipped);
    if (fromIndex < 0 || fromIndex >= items.size() || items.get(fromIndex) == null) return;
    Item item = items.get(fromIndex);
    InventoryComponent inventory = inventoryDisplay.getInventoryComponent();
    if (item instanceof Charm charm) {
      inventory.setCharmEquipped(charm, equipped);
    } else if (item instanceof ConsumableItem) {
      if (equipped) {
        if (toIndex < 0) {
          for (int i = 0; i < InventoryComponent.CONSUMABLE_SLOT_COUNT; i++) {
            if (inventory.getConsumableSlot(i) == null) {
              toIndex = i;
              break;
            }
          }
        }
        if (toIndex < 0 || toIndex >= InventoryComponent.CONSUMABLE_SLOT_COUNT) return;
        inventory.equipConsumable(item.getId(), toIndex);
      } else {
        inventory.unequipConsumable(fromIndex);
      }
    }
    inventoryDisplay.refreshPage();
  }
}
