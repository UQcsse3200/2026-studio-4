package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.items.ItemIds;
import org.junit.jupiter.api.Test;

class ConsumableSelectionComponentTest {
  @Test
  void callersCannotReplaceTheSelectedSlotWithANonConsumable() {
    ConsumableSelectionComponent selection = new ConsumableSelectionComponent();

    assertThrows(
        UnsupportedOperationException.class,
        () -> ConsumableSelectionComponent.SLOTS.set(0, ItemIds.GOLD_COIN));

    assertEquals(ItemIds.HEALTH_POTION, selection.getSelectedType());
  }
}
