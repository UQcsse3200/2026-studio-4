package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.csse3200.game.components.player.Team5CombatHudDisplay.ConsumableSlot;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class Team5CombatHudDisplayTest {
  @Test
  void shouldFormatGoldAndClampNegativeValues() {
    assertEquals("Gold: 12", Team5CombatHudDisplay.formatGold(12));
    assertEquals("Gold: 0", Team5CombatHudDisplay.formatGold(-1));
  }

  @Test
  void shouldFormatConsumableSlotsAndClampNegativeCounts() {
    assertEquals("Health x3", Team5CombatHudDisplay.formatSlot(ConsumableSlot.HEALTH, 3));
    assertEquals("Shield x0", Team5CombatHudDisplay.formatSlot(ConsumableSlot.SHIELD, -1));
    assertEquals("Speed x2", Team5CombatHudDisplay.formatSlot(ConsumableSlot.SPEED, 2));
    assertEquals("Strength x1", Team5CombatHudDisplay.formatSlot(ConsumableSlot.STRENGTH, 1));
  }

  @Test
  void shouldFormatSelectedConsumable() {
    assertEquals("Last used: None", Team5CombatHudDisplay.formatSelected(null));
    assertEquals(
        "Last used: Strength", Team5CombatHudDisplay.formatSelected(ConsumableSlot.STRENGTH));
  }

  @Test
  void shouldMapItemTypesToHudSlots() {
    assertEquals(ConsumableSlot.HEALTH, ConsumableSlot.fromItemType(ItemType.HEALTH_POTION));
    assertEquals(ConsumableSlot.SHIELD, ConsumableSlot.fromItemType(ItemType.SHIELD));
    assertEquals(ConsumableSlot.SPEED, ConsumableSlot.fromItemType(ItemType.SPEED_POTION));
    assertEquals(ConsumableSlot.STRENGTH, ConsumableSlot.fromItemType(ItemType.STRENGTH_POTION));
  }

  @Test
  void shouldIgnoreNonConsumableAndMissingSelections() {
    assertNull(ConsumableSlot.fromItemType(ItemType.GOLD_COIN));
    assertNull(ConsumableSlot.fromItemType(ItemType.STRENGTH_CHARM));
    assertNull(ConsumableSlot.fromItemType(null));
  }

  @Test
  void shouldUpdateMatchingSlotWhenInventoryEventFires() {
    Team5CombatHudDisplay hud = spy(new Team5CombatHudDisplay());
    Entity player = new Entity().addComponent(hud);
    hud.registerEventListeners();

    player.getEvents().trigger("consumableInventoryChanged", ItemType.SPEED_POTION, 3);

    verify(hud).updateConsumableCount(ConsumableSlot.SPEED, 3);
  }

  @Test
  void shouldReflectConsumablesAddedToAndRemovedFromInventory() {
    InventoryComponent inventory = new InventoryComponent(0);
    Team5CombatHudDisplay hud = spy(new Team5CombatHudDisplay());
    new Entity().addComponent(inventory).addComponent(hud);
    hud.registerEventListeners();

    inventory.addConsumable(ItemType.HEALTH_POTION);
    inventory.removeConsumable(ItemType.HEALTH_POTION);

    verify(hud).updateConsumableCount(ConsumableSlot.HEALTH, 1);
    verify(hud).updateConsumableCount(ConsumableSlot.HEALTH, 0);
  }

  @Test
  void shouldReadGoldFromInventoryDuringUpdate() {
    InventoryComponent inventory = new InventoryComponent(25);
    Team5CombatHudDisplay hud = spy(new Team5CombatHudDisplay());
    new Entity().addComponent(inventory).addComponent(hud);

    hud.update();

    verify(hud).updateGold(25);
  }

  @Test
  void shouldUpdateSelectionWhenSelectionEventFires() {
    Team5CombatHudDisplay hud = spy(new Team5CombatHudDisplay());
    Entity player = new Entity().addComponent(hud);
    hud.registerEventListeners();

    player.getEvents().trigger(ConsumableEffectComponent.USED, ItemType.SHIELD);

    verify(hud).updateSelectedConsumable(ConsumableSlot.SHIELD);
  }
}
