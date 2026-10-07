package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.charms.Charm;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class InventoryComponentTest {
  @Test
  void shouldPurchaseOneConsumable() {
    InventoryComponent inventory = new InventoryComponent(25);
    assertEquals(
        ConsumablePurchaseResult.SUCCESS,
        inventory.tryPurchaseConsumable(ItemIds.HEALTH_POTION, 10));
    assertEquals(15, inventory.getGold());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertEquals(
        ConsumablePurchaseResult.SUCCESS,
        inventory.tryPurchaseConsumable(ItemIds.HEALTH_POTION, 10));
    assertEquals(5, inventory.getGold());
    assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertEquals(
        ConsumablePurchaseResult.INSUFFICIENT_GOLD,
        inventory.tryPurchaseConsumable(ItemIds.HEALTH_POTION, 10));
    assertEquals(5, inventory.getGold());
    assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void shouldRejectPurchaseWithoutChangingInventory() {
    InventoryComponent inventory = new InventoryComponent(25);
    Entity player = new Entity().addComponent(inventory);
    List<Integer> notifications = new ArrayList<>();
    player.getEvents().<Integer>addListener("goldChanged", notifications::add);
    player
        .getEvents()
        .addListener(
            "consumableInventoryChanged", (String id, Integer count) -> notifications.add(count));
    for (String id : new String[] {null, "missing", ItemIds.STRENGTH_CHARM}) {
      assertEquals(ConsumablePurchaseResult.INVALID_ITEM, inventory.tryPurchaseConsumable(id, 10));
    }
    for (int price : new int[] {0, -1, Integer.MIN_VALUE}) {
      assertEquals(
          ConsumablePurchaseResult.INVALID_PRICE,
          inventory.tryPurchaseConsumable(ItemIds.HEALTH_POTION, price));
    }
    assertEquals(
        ConsumablePurchaseResult.INSUFFICIENT_GOLD,
        inventory.tryPurchaseConsumable(ItemIds.HEALTH_POTION, 26));
    assertEquals(25, inventory.getGold());
    assertEquals(0, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertTrue(notifications.isEmpty());
  }

  @Test
  void shouldRejectQuantityOverflow() {
    InventoryComponent inventory = new InventoryComponent(25);
    inventory.addConsumable(ItemIds.HEALTH_POTION, Integer.MAX_VALUE);
    assertEquals(
        ConsumablePurchaseResult.QUANTITY_LIMIT,
        inventory.tryPurchaseConsumable(ItemIds.HEALTH_POTION, 10));
    assertEquals(25, inventory.getGold());
    assertEquals(Integer.MAX_VALUE, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void shouldPublishCommittedPurchaseState() {
    InventoryComponent inventory = new InventoryComponent(25);
    Entity player = new Entity().addComponent(inventory);
    List<String> states = new ArrayList<>();
    player
        .getEvents()
        .addListener(
            "goldChanged",
            (Integer gold) -> {
              assertEquals(15, gold);
              states.add(
                  inventory.getGold() + ":" + inventory.getConsumableCount(ItemIds.HEALTH_POTION));
            });
    player
        .getEvents()
        .addListener(
            "consumableInventoryChanged",
            (String id, Integer count) -> {
              assertEquals(ItemIds.HEALTH_POTION, id);
              assertEquals(1, count);
              states.add(inventory.getGold() + ":" + inventory.getConsumableCount(id));
            });
    inventory.tryPurchaseConsumable(ItemIds.HEALTH_POTION, 10);
    assertEquals(List.of("15:1", "15:1"), states);
  }

  @Test
  void shouldNotifyActualGoldChangesOnly() {
    InventoryComponent inventory = new InventoryComponent(25);
    Entity player = new Entity().addComponent(inventory);
    List<Integer> balances = new ArrayList<>();
    player.getEvents().<Integer>addListener("goldChanged", balances::add);
    inventory.setGold(25);
    inventory.setGold(10);
    inventory.addGold(-50);
    inventory.setGold(-1);
    assertEquals(List.of(10, 0), balances);
  }

  @Test
  void shouldPublishCurrentQuantityAfterGoldListenerAddsAnItem() {
    InventoryComponent inventory = new InventoryComponent(25);
    Entity player = new Entity().addComponent(inventory);
    List<Integer> quantities = new ArrayList<>();
    player
        .getEvents()
        .addListener(
            "goldChanged", (Integer gold) -> inventory.addConsumable(ItemIds.HEALTH_POTION));
    player
        .getEvents()
        .addListener(
            "consumableInventoryChanged", (String id, Integer count) -> quantities.add(count));

    assertEquals(
        ConsumablePurchaseResult.SUCCESS,
        inventory.tryPurchaseConsumable(ItemIds.HEALTH_POTION, 10));

    assertEquals(15, inventory.getGold());
    assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertEquals(List.of(2, 2), quantities);
  }

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
    Charm charm = mock(Charm.class);
    inventory.addCharm(charm);

    assertEquals(1, inventory.getCharmCount());
  }

  @Test
  void shouldRemoveCharm() {
    InventoryComponent inventory = new InventoryComponent(100);
    Charm charm = mock(Charm.class);
    inventory.addCharm(charm);
    assertTrue(inventory.removeCharm(charm));

    assertFalse(inventory.hasCharm(charm));
    assertEquals(0, inventory.getCharmCount());
  }

  @Test
  void shouldCheckHasCharm() {
    InventoryComponent inventory = new InventoryComponent(100);
    Charm charm = mock(Charm.class);

    assertFalse(inventory.hasCharm(charm));

    inventory.addCharm(charm);

    assertTrue(inventory.hasCharm(charm));
  }

  @Test
  void shouldAddConsumable() {
    InventoryComponent inventory = new InventoryComponent(0);

    inventory.addConsumable(ItemIds.HEALTH_POTION);

    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertTrue(inventory.hasConsumable(ItemIds.HEALTH_POTION));
  }

  @Test
  void shouldStackConsumables() {
    InventoryComponent inventory = new InventoryComponent(0);

    inventory.addConsumable(ItemIds.HEALTH_POTION);
    inventory.addConsumable(ItemIds.HEALTH_POTION);

    assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void shouldAddCallerSelectedConsumableQuantity() {
    InventoryComponent inventory = new InventoryComponent(0);

    inventory.addConsumable(ItemIds.HEALTH_POTION, 3);

    assertEquals(3, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void shouldRemoveConsumable() {
    InventoryComponent inventory = new InventoryComponent(0);

    inventory.addConsumable(ItemIds.HEALTH_POTION);
    boolean removed = inventory.removeConsumable(ItemIds.HEALTH_POTION);

    assertTrue(removed);
    assertEquals(0, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertFalse(inventory.hasConsumable(ItemIds.HEALTH_POTION));
  }

  @Test
  void shouldNotRemoveMissingConsumable() {
    InventoryComponent inventory = new InventoryComponent(0);

    boolean removed = inventory.removeConsumable(ItemIds.HEALTH_POTION);

    assertFalse(removed);
    assertEquals(0, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void shouldStoreDifferentConsumablesSeparately() {
    InventoryComponent inventory = new InventoryComponent(0);

    inventory.addConsumable(ItemIds.HEALTH_POTION);
    inventory.addConsumable(ItemIds.SPEED_POTION);
    inventory.addConsumable(ItemIds.SPEED_POTION);

    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertEquals(2, inventory.getConsumableCount(ItemIds.SPEED_POTION));
    assertEquals(0, inventory.getConsumableCount(ItemIds.SHIELD));
  }
}
