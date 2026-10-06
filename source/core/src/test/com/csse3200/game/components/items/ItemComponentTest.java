package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.charms.Charm;
import com.csse3200.game.items.charms.SpeedCharm;
import com.csse3200.game.items.charms.StrengthCharm;
import org.junit.jupiter.api.Test;

class ItemComponentTest {
  @Test
  void shouldStoreSharedItemAndTeam5Metadata() {
    Charm charm = new StrengthCharm();

    ItemComponent component = new ItemComponent(charm);

    assertSame(charm, component.getItem());
    assertSame(charm, component.getCharm());
    assertEquals(ItemIds.STRENGTH_CHARM, component.getItemId());
    assertEquals(1, component.getQuantity());
  }

  @Test
  void shouldStoreNonCharmItemId() {
    ItemComponent component = new ItemComponent(ItemIds.HEALTH_POTION);

    assertEquals(ItemIds.HEALTH_POTION, component.getItemId());
    assertNull(component.getCharm());
    assertEquals(1, component.getQuantity());
  }

  @Test
  void shouldRecogniseEveryCharmThroughTheSharedItemId() {
    ItemComponent component = new ItemComponent(new SpeedCharm());
    assertEquals(ItemIds.SPEED_CHARM, component.getItemId());
    assertSame(component.getItem(), component.getCharm());
  }

  @Test
  void shouldStoreCallerSelectedQuantity() {
    ItemComponent component = new ItemComponent(new ItemDropSpec(ItemIds.GOLD_COIN, 25));

    assertEquals(ItemIds.GOLD_COIN, component.getItemId());
    assertEquals(25, component.getQuantity());
  }

  @Test
  void shouldRejectInvalidConstruction() {
    assertThrows(NullPointerException.class, () -> new ItemComponent((Charm) null));
    assertThrows(NullPointerException.class, () -> new ItemComponent((String) null));
    assertThrows(NullPointerException.class, () -> new ItemComponent((ItemDropSpec) null));
    assertEquals(ItemIds.STRENGTH_CHARM, new ItemComponent(ItemIds.STRENGTH_CHARM).getItemId());
  }
}
