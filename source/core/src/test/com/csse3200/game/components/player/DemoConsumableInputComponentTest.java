package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.items.ItemType;
import org.junit.jupiter.api.Test;

class DemoConsumableInputComponentTest {
  @Test
  void shouldMapNumberKeysToConsumables() {
    assertEquals(ItemType.HEALTH_POTION, DemoConsumableInputComponent.itemTypeForKey(Keys.NUM_1));
    assertEquals(ItemType.SHIELD, DemoConsumableInputComponent.itemTypeForKey(Keys.NUM_2));
    assertEquals(ItemType.SPEED_POTION, DemoConsumableInputComponent.itemTypeForKey(Keys.NUM_3));
    assertEquals(ItemType.STRENGTH_POTION, DemoConsumableInputComponent.itemTypeForKey(Keys.NUM_4));
    assertNull(DemoConsumableInputComponent.itemTypeForKey(Keys.NUM_5));
  }
}
