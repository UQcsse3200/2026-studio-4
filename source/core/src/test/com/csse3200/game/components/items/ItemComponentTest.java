package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.items.charms.Charm;
import com.csse3200.game.items.charms.StrengthCharm;
import org.junit.jupiter.api.Test;

class ItemComponentTest {
  @Test
  void shouldStoreItem() {
    Charm charm = new StrengthCharm();

    ItemComponent component = new ItemComponent(charm);

    assertSame(charm, component.getItem());
  }

  @Test
  void shouldRejectNullCharm() {
    assertThrows(NullPointerException.class, () -> new ItemComponent(null));
  }
}
