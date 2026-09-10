package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.items.Charm;
import com.csse3200.game.items.ItemType;
import org.junit.jupiter.api.Test;

class ItemComponentTest {
  @Test
  void shouldStoreCharm() {
    Charm charm = new Charm("Test Charm");

    ItemComponent component = new ItemComponent(charm);

    assertSame(charm, component.getCharm());
    assertEquals(ItemType.STRENGTH_CHARM, component.getItemType());
  }

  @Test
  void shouldStoreNonCharmItemType() {
    ItemComponent component = new ItemComponent(ItemType.HEALTH_POTION);

    assertEquals(ItemType.HEALTH_POTION, component.getItemType());
    assertNull(component.getCharm());
  }

  @Test
  void shouldRejectNullCharm() {
    assertThrows(NullPointerException.class, () -> new ItemComponent((Charm) null));
    assertThrows(NullPointerException.class, () -> new ItemComponent((ItemType) null));
  }

  @Test
  void shouldRequireCharmDataForCharmTypes() {
    assertThrows(IllegalArgumentException.class, () -> new ItemComponent(ItemType.STRENGTH_CHARM));
  }
}
