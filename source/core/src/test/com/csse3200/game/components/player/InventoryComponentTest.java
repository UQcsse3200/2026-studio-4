package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.StatCharm;
import com.csse3200.game.items.StrengthCharm;
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
  void shouldAddCharmAndApplyStats() {
    Entity player = mock(Entity.class);
    CombatStatsComponent stat = mock(CombatStatsComponent.class);

    when(player.getComponent(CombatStatsComponent.class)).thenReturn(stat);

    InventoryComponent inventory = new InventoryComponent(100);
    inventory.setEntity(player);

    StatCharm<?> charm = mock(StatCharm.class);

    inventory.addCharm(charm);

    verify(charm).applyStatChange(stat);
    assertTrue(inventory.hasCharm(charm));
    assertEquals(1, inventory.getCharmCount());
  }

  @Test
  void shouldRemoveCharmAndRemoveStats() {
    Entity player = mock(Entity.class);
    CombatStatsComponent stat = mock(CombatStatsComponent.class);
    StatCharm<?> charm = mock(StatCharm.class);
    when(player.getComponent(CombatStatsComponent.class)).thenReturn(stat);

    InventoryComponent inventory = new InventoryComponent(100);

    inventory.setEntity(player);
    inventory.addCharm(charm);
    assertTrue(inventory.removeCharm(charm));

    verify(charm).removeStatChange(stat);
    assertFalse(inventory.hasCharm(charm));
    assertEquals(0, inventory.getCharmCount());
  }

  @Test
  void shouldCheckHasCharm() {
    InventoryComponent inventory = new InventoryComponent(100);
    StatCharm<?> charm = new StrengthCharm();

    assertFalse(inventory.hasCharm(charm));

    inventory.addCharm(charm);

    assertTrue(inventory.hasCharm(charm));
  }
}
