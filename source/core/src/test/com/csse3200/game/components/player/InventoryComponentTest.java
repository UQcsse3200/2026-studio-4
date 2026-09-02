package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.Charm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class InventoryComponentTest {
  @Test
  void shouldSetGetGold() {
    InventoryComponent inventory = new InventoryComponent(100);
    assertEquals(100, inventory.getGold());

    inventory.setGold(150);
    assertEquals(150, inventory.getGold());

    inventory.setGold(-50);
    assertEquals(0, inventory.getGold());
  }

  @Test
  void shouldCheckHasGold() {
    InventoryComponent inventory = new InventoryComponent(150);
    assertTrue(inventory.hasGold(100));
    assertFalse(inventory.hasGold(200));
  }

  @Test
  void shouldAddGold() {
    InventoryComponent inventory = new InventoryComponent(100);
    inventory.addGold(-500);
    assertEquals(0, inventory.getGold());

    inventory.addGold(100);
    inventory.addGold(-20);
    assertEquals(80, inventory.getGold());
  }

  @Test
  void shouldStartWithEmptyCharmInventory() {
    InventoryComponent inventory = new InventoryComponent(100);

    assertEquals(0, inventory.getCharmCount());
    assertTrue(inventory.getCharms().isEmpty());
  }

  @Test
  void shouldAddCharm() {
    InventoryComponent inventory = new InventoryComponent(100);
    Charm charm = new Charm("Strength Charm");

    inventory.addCharm(charm);

    assertTrue(inventory.hasCharm(charm));
    assertEquals(1, inventory.getCharmCount());
  }

  @Test
  void shouldRemoveCharm() {
    InventoryComponent inventory = new InventoryComponent(100);
    Charm charm = new Charm("Strength Charm");

    inventory.addCharm(charm);
    assertTrue(inventory.removeCharm(charm));

    assertFalse(inventory.hasCharm(charm));
    assertEquals(0, inventory.getCharmCount());
  }

  @Test
  void shouldCheckHasCharm() {
    InventoryComponent inventory = new InventoryComponent(100);
    Charm charm = new Charm("Strength Charm");

    assertFalse(inventory.hasCharm(charm));

    inventory.addCharm(charm);

    assertTrue(inventory.hasCharm(charm));
  }
}
