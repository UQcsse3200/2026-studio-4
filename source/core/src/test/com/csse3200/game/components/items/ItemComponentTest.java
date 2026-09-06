package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.items.StatCharm;
import com.csse3200.game.items.StrengthCharm;
import org.junit.jupiter.api.Test;

class ItemComponentTest {
  @Test
  void shouldStoreCharm() {
    StatCharm<?> charm = new StrengthCharm();

    ItemComponent component = new ItemComponent(charm);

    assertSame(charm, component.getCharm());
  }

  @Test
  void shouldRejectNullCharm() {
    assertThrows(NullPointerException.class, () -> new ItemComponent(null));
  }
}
