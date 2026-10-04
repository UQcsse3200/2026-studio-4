package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemType;
import org.junit.jupiter.api.Test;

class ConsumableSelectionComponentTest {
  @Test
  void firstPickupDeterminesSelectedItemEvenBeforePlayerCreation() {
    InventoryComponent inventory = new InventoryComponent(0);
    inventory.addConsumable(ItemType.SHIELD);
    inventory.addConsumable(ItemType.STRENGTH_POTION);
    ConsumableSelectionComponent selection = new ConsumableSelectionComponent();
    Entity player = new Entity().addComponent(inventory).addComponent(selection);
    player.create();
    assertEquals(ItemType.SHIELD, selection.getSelectedType());
    selection.cycle();
    assertEquals(ItemType.STRENGTH_POTION, selection.getSelectedType());
    inventory.removeConsumable(ItemType.SHIELD);
    inventory.addConsumable(ItemType.SPEED_POTION);
    assertEquals(ItemType.STRENGTH_POTION, selection.getSelectedType());
    selection.cycle();
    assertNull(selection.getSelectedType());
    selection.cycle();
    assertNull(selection.getSelectedType());
    selection.cycle();
    assertEquals(ItemType.SPEED_POTION, selection.getSelectedType());
  }
}
