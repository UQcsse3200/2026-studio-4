package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemFactoryTest {
  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
  }

  @Test
  void shouldCreateStrengthCharmForEveryDropRequest() {
    Entity firstDrop = ItemFactory.createDrop();
    Entity secondDrop = ItemFactory.createDrop();

    assertNotNull(firstDrop);
    assertNotNull(secondDrop);
    assertEquals(
        "Strength Charm", firstDrop.getComponent(ItemComponent.class).getCharm().getName());
    assertEquals(
        "Strength Charm", secondDrop.getComponent(ItemComponent.class).getCharm().getName());
    assertNotSame(firstDrop, secondDrop);
  }

  @Test
  void shouldCreateStrengthCharm() {
    Entity item = ItemFactory.createStrengthCharm();

    ItemComponent itemComponent = item.getComponent(ItemComponent.class);
    assertNotNull(itemComponent);
    assertEquals("Strength Charm", itemComponent.getCharm().getName());
    assertNotNull(item.getComponent(PhysicsComponent.class));
    assertNotNull(item.getComponent(HitboxComponent.class));
    assertEquals(PhysicsLayer.ITEM, item.getComponent(HitboxComponent.class).getLayer());
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
