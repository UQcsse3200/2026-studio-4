package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ItemTypeTest {
  @Test
  void shouldProvideCompleteMetadataForEveryItem() {
    for (ItemType itemType : ItemType.values()) {
      assertFalse(itemType.getDisplayName().isBlank());
      assertFalse(itemType.getDescription().isBlank());
      assertTrue(itemType.getTexturePath().startsWith("images/"));
      assertNotNull(itemType.getCategory());
    }
  }

  @Test
  void shouldClassifyConsumablesAndCurrency() {
    assertTrue(ItemType.HEALTH_POTION.isConsumable());
    assertTrue(ItemType.SHIELD.isConsumable());
    assertTrue(ItemType.SPEED_POTION.isConsumable());
    assertTrue(ItemType.STRENGTH_POTION.isConsumable());
    assertFalse(ItemType.STRENGTH_CHARM.isConsumable());

    assertTrue(ItemType.GOLD_COIN.isCurrency());
    assertFalse(ItemType.HEALTH_POTION.isCurrency());
  }
}
