package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ItemDropSpecTest {
  @Test
  void shouldStoreCallerSelectedItemAndQuantity() {
    ItemDropSpec dropSpec = new ItemDropSpec(ItemIds.GOLD_COIN, 10);

    assertEquals(ItemIds.GOLD_COIN, dropSpec.itemId());
    assertEquals(10, dropSpec.quantity());
  }

  @Test
  void shouldCreateSingleUnitDrop() {
    assertEquals(1, ItemDropSpec.single(ItemIds.HEALTH_POTION).quantity());
  }

  @Test
  void shouldRejectInvalidDropSpecifications() {
    assertThrows(NullPointerException.class, () -> new ItemDropSpec(null, 1));
    assertThrows(IllegalArgumentException.class, () -> new ItemDropSpec(ItemIds.GOLD_COIN, 0));
    assertThrows(IllegalArgumentException.class, () -> new ItemDropSpec(ItemIds.HEALTH_POTION, -1));
  }
}
