package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.items.consumables.InstantHealingPotion;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItemCatalogTest {
  @Test
  void everyRegisteredIdCreatesAnItemWithItsOwnMetadata() {
    for (String id : ItemCatalog.ids()) {
      Item item = ItemCatalog.create(id, 1);
      assertEquals(id, item.getId());
      assertFalse(item.getName().isBlank());
      assertFalse(item.getDescription().isBlank());
      assertTrue(item.getTexture().startsWith("images/"));
      assertNotNull(item.getCategory());
    }
  }

  @Test
  void healingSizesShareOneClassButHaveDistinctStableIdsAndAmounts() {
    Item small = ItemCatalog.create(ItemIds.HEALTH_POTION, 1);
    Item medium = ItemCatalog.create(ItemIds.MEDIUM_HEALTH_POTION, 1);
    Item large = ItemCatalog.create(ItemIds.LARGE_HEALTH_POTION, 1);
    assertEquals("Small Health Potion", small.getName());
    assertFalse(small.getId().equals(small.getName()));
    assertEquals(InstantHealingPotion.class, small.getClass());
    assertEquals(small.getClass(), medium.getClass());
    assertEquals(medium.getClass(), large.getClass());
    assertFalse(small.getId().equals(medium.getId()));
    assertFalse(medium.getId().equals(large.getId()));
  }

  @Test
  void arbitraryPositiveHealingAmountRoundTripsThroughStableId() {
    InstantHealingPotion custom = new InstantHealingPotion(2, 75);
    assertEquals("HEALTH_POTION_75", custom.getId());
    assertEquals(75, custom.getHealing());
    assertEquals(2, custom.getQuantity());
    assertTrue(ItemCatalog.contains(custom.getId()));
    assertEquals(75, ((InstantHealingPotion) ItemCatalog.create(custom.getId(), 1)).getHealing());
    assertEquals(custom.getId(), ItemCatalog.createItems(custom.getId(), 3).get(0).getId());
    assertEquals(3, ItemCatalog.createItems(custom.getId(), 3).get(0).getQuantity());
    assertEquals(custom.getId(), new ItemDropSpec(custom.getId(), 2).itemId());
  }

  @Test
  void legacyHealingIdsKeepTheirNamesAndCustomIdMustBeCanonical() {
    assertEquals(ItemIds.HEALTH_POTION, new InstantHealingPotion(1, 25).getId());
    assertEquals(ItemIds.MEDIUM_HEALTH_POTION, new InstantHealingPotion(1, 50).getId());
    assertEquals(ItemIds.LARGE_HEALTH_POTION, new InstantHealingPotion(1, 100).getId());
    for (String invalid :
        List.of(
            "HEALTH_POTION_0",
            "HEALTH_POTION_-75",
            "HEALTH_POTION_075",
            "HEALTH_POTION_25",
            "HEALTH_POTION_2147483648")) {
      assertFalse(ItemCatalog.contains(invalid));
      assertThrows(IllegalArgumentException.class, () -> ItemCatalog.create(invalid, 1));
    }
    assertThrows(IllegalArgumentException.class, () -> new InstantHealingPotion(1, 0));
  }

  @Test
  void rejectsUnknownIdsAndInvalidQuantities() {
    assertThrows(IllegalArgumentException.class, () -> ItemCatalog.create("UNKNOWN", 1));
    assertThrows(IllegalArgumentException.class, () -> ItemCatalog.create(ItemIds.GOLD_COIN, 0));
    assertThrows(IllegalArgumentException.class, () -> ItemCatalog.create(ItemIds.SPEED_CHARM, 2));
    assertThrows(
        IllegalArgumentException.class, () -> ItemCatalog.createItems(ItemIds.SPEED_CHARM, 0));
  }

  @Test
  void multipleCharmsAreIndependentWhileConsumablesAndGoldRemainStacks() {
    List<Item> charms = ItemCatalog.createItems(ItemIds.SPEED_CHARM, 2);
    assertEquals(2, charms.size());
    assertNotSame(charms.get(0), charms.get(1));
    assertEquals(1, charms.get(0).getQuantity());
    assertEquals(1, charms.get(1).getQuantity());

    List<Item> potions = ItemCatalog.createItems(ItemIds.HEALTH_POTION, 2);
    assertEquals(1, potions.size());
    assertEquals(2, potions.get(0).getQuantity());
    List<Item> coins = ItemCatalog.createItems(ItemIds.GOLD_COIN, 3);
    assertEquals(1, coins.size());
    assertEquals(3, coins.get(0).getQuantity());
  }
}
