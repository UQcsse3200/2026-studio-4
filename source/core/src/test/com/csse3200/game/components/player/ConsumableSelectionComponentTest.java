package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemIds;
import org.junit.jupiter.api.Test;

class ConsumableSelectionComponentTest {
  @Test
  void firstPickupDeterminesSelectedItemEvenBeforePlayerCreation() {
    InventoryComponent inventory = new InventoryComponent(0);
    inventory.addConsumable(ItemIds.SHIELD);
    inventory.addConsumable(ItemIds.STRENGTH_POTION);
    ConsumableSelectionComponent selection = new ConsumableSelectionComponent();
    Entity player = new Entity().addComponent(inventory).addComponent(selection);
    player.create();
    assertEquals(ItemIds.SHIELD, selection.getSelectedType());
    selection.cycle();
    assertEquals(ItemIds.STRENGTH_POTION, selection.getSelectedType());
    inventory.removeConsumable(ItemIds.SHIELD);
    inventory.addConsumable(ItemIds.SPEED_POTION);
    assertEquals(ItemIds.STRENGTH_POTION, selection.getSelectedType());
    selection.cycle();
    assertNull(selection.getSelectedType());
    selection.cycle();
    assertNull(selection.getSelectedType());
    selection.cycle();
    assertEquals(ItemIds.SPEED_POTION, selection.getSelectedType());
  }
}
