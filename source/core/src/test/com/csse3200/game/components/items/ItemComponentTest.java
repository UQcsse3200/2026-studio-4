package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.Item;
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

  @Test
  void shouldApplyPickupOnlyOnceAcrossCollectors() {
    Item item = mock(Item.class);
    when(item.getQuantity()).thenReturn(1);
    ItemComponent component = new ItemComponent(item);
    Entity firstCollector = new Entity();
    Entity secondCollector = new Entity();

    assertFalse(component.isCollected());
    assertTrue(component.tryPickUp(firstCollector));
    assertTrue(component.isCollected());
    assertFalse(component.tryPickUp(firstCollector));
    assertFalse(component.tryPickUp(secondCollector));

    verify(item, times(1)).pickUp(firstCollector);
    verify(item, never()).pickUp(secondCollector);
  }

  @Test
  void shouldRejectPickupAfterDisposal() {
    Item item = mock(Item.class);
    when(item.getQuantity()).thenReturn(1);
    ItemComponent component = new ItemComponent(item);
    Entity collector = new Entity();

    component.dispose();

    assertTrue(component.isCollected());
    assertFalse(component.tryPickUp(collector));
    verify(item, never()).pickUp(collector);
  }
}
