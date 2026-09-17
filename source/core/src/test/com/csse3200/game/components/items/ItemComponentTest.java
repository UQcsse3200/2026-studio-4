package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.items.charms.Charm;
import com.csse3200.game.items.charms.StrengthCharm;
import org.junit.jupiter.api.Test;

class ItemComponentTest {
  @Test
  void shouldStoreSharedItemAndTeam5Metadata() {
    Charm charm = new StrengthCharm();

    ItemComponent component = new ItemComponent(charm);

    assertSame(charm, component.getItem());
    assertSame(charm, component.getCharm());
    assertEquals(ItemType.STRENGTH_CHARM, component.getItemType());
    assertEquals(1, component.getQuantity());
  }

  @Test
  void shouldStoreNonCharmItemType() {
    ItemComponent component = new ItemComponent(ItemType.HEALTH_POTION);

    assertEquals(ItemType.HEALTH_POTION, component.getItemType());
    assertNull(component.getCharm());
    assertEquals(1, component.getQuantity());
  }

  @Test
  void shouldStoreCallerSelectedQuantity() {
    ItemComponent component = new ItemComponent(new ItemDropSpec(ItemType.GOLD_COIN, 25));

    assertEquals(ItemType.GOLD_COIN, component.getItemType());
    assertEquals(25, component.getQuantity());
  }

  @Test
  void shouldRejectInvalidConstruction() {
    assertThrows(NullPointerException.class, () -> new ItemComponent((Charm) null));
    assertThrows(NullPointerException.class, () -> new ItemComponent((ItemType) null));
    assertThrows(NullPointerException.class, () -> new ItemComponent((ItemDropSpec) null));
    assertThrows(IllegalArgumentException.class, () -> new ItemComponent(ItemType.STRENGTH_CHARM));
  }
}
