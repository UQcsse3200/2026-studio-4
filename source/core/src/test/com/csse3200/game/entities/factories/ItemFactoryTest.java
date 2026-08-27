package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.Test;

class ItemFactoryTest {
  @Test
  void shouldCreateStrengthCharm() {
    Entity item = ItemFactory.createStrengthCharm();

    ItemComponent itemComponent = item.getComponent(ItemComponent.class);
    assertNotNull(itemComponent);
    assertEquals("Strength Charm", itemComponent.getCharm().getName());
  }

  @Test
  void shouldCreateIndependentStrengthCharms() {
    Entity firstItem = ItemFactory.createStrengthCharm();
    Entity secondItem = ItemFactory.createStrengthCharm();

    assertNotSame(firstItem, secondItem);
    assertNotSame(
        firstItem.getComponent(ItemComponent.class).getCharm(),
        secondItem.getComponent(ItemComponent.class).getCharm());
  }
}
