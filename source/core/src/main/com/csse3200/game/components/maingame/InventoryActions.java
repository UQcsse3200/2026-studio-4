package com.csse3200.game.components.maingame;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryActions extends Component {
  private static final Logger logger = LoggerFactory.getLogger(InventoryActions.class);
  private final InventoryDisplay inventoryDisplay;
  private InventoryComponent inventoryComponent;

  public InventoryActions(InventoryDisplay inventoryDisplay, InventoryComponent inventoryComponent) {
    this.inventoryDisplay = inventoryDisplay;
    this.inventoryComponent = inventoryComponent;
  }

  public InventoryActions(InventoryDisplay inventoryDisplay) {
    this.inventoryDisplay = inventoryDisplay;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("nextPage", this::nextPage);
    entity.getEvents().addListener("moveActiveToInactiveItem", this::moveActiveToInactiveItem);
    entity.getEvents().addListener("moveInactiveToActiveItem", this::moveInactiveToActiveItem);
  }

  /** Changes page from the current inventory page to the non-displayed page */
  private void nextPage() {
    logger.info("Swap inventory page");
    inventoryDisplay.changePage();
  }

  private void moveActiveToInactiveItem (int fromIndex, int toIndex) {
    logger.info("Move an active item to an inactive item");
  }

  private void moveInactiveToActiveItem (int fromIndex, int toIndex) {
    logger.info("Move an inactive item to an active item");
  }
}
