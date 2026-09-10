package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ItemDropSpecTest {
  @Test
  void shouldStoreCallerSelectedItemAndQuantity() {
    ItemDropSpec dropSpec = new ItemDropSpec(ItemType.GOLD_COIN, 10);

    assertEquals(ItemType.GOLD_COIN, dropSpec.itemType());
    assertEquals(10, dropSpec.quantity());
  }

  @Test
  void shouldCreateSingleUnitDrop() {
    assertEquals(1, ItemDropSpec.single(ItemType.HEALTH_POTION).quantity());
  }

  @Test
  void shouldRejectInvalidDropSpecifications() {
    assertThrows(NullPointerException.class, () -> new ItemDropSpec(null, 1));
    assertThrows(IllegalArgumentException.class, () -> new ItemDropSpec(ItemType.GOLD_COIN, 0));
    assertThrows(
        IllegalArgumentException.class, () -> new ItemDropSpec(ItemType.HEALTH_POTION, -1));
  }
}
