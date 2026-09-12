package com.csse3200.game.components.maingame;

import com.csse3200.game.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryActions extends Component {
    private static final Logger logger = LoggerFactory.getLogger(InventoryActions.class);
    private InventoryDisplay inventoryDisplay;

    public InventoryActions(InventoryDisplay inventoryDisplay) {
        this.inventoryDisplay = inventoryDisplay;
    }

    public void create() {entity.getEvents().addListener("nextPage", this::nextPage);}


    private void nextPage() {
        logger.info("Swap inventory page");
        inventoryDisplay.changePage();

    }
}
