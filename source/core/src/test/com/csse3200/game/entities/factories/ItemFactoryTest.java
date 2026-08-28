package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ResourceService;
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
    ResourceService resourceService = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(resourceService.getAsset("images/strength_charm_pixel.png", Texture.class))
        .thenReturn(texture);
    when(texture.getWidth()).thenReturn(1270);
    when(texture.getHeight()).thenReturn(1239);
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void shouldCreateStrengthCharmForEveryDropRequest() {
    Vector2 firstPosition = new Vector2(1f, 2f);
    Vector2 secondPosition = new Vector2(3f, 4f);
    Entity firstDrop = ItemFactory.createDrop(ItemType.STRENGTH_CHARM, firstPosition);
    Entity secondDrop = ItemFactory.createDrop(ItemType.STRENGTH_CHARM, secondPosition);

    assertNotNull(firstDrop);
    assertNotNull(secondDrop);
    assertEquals(
        "Strength Charm", firstDrop.getComponent(ItemComponent.class).getCharm().getName());
    assertEquals(
        "Strength Charm", secondDrop.getComponent(ItemComponent.class).getCharm().getName());
    assertEquals(firstPosition, firstDrop.getPosition());
    assertEquals(secondPosition, secondDrop.getPosition());
    assertNotSame(firstDrop, secondDrop);
  }

  @Test
  void shouldRejectInvalidDropRequest() {
    assertThrows(
        NullPointerException.class, () -> ItemFactory.createDrop(null, new Vector2(1f, 2f)));
    assertThrows(
        NullPointerException.class, () -> ItemFactory.createDrop(ItemType.STRENGTH_CHARM, null));
  }

  @Test
  void shouldCreateStrengthCharm() {
    Entity item = ItemFactory.createStrengthCharm();

    ItemComponent itemComponent = item.getComponent(ItemComponent.class);
    assertEquals((short) (1 << 5), PhysicsLayer.ITEM);
    assertNotNull(itemComponent);
    assertEquals("Strength Charm", itemComponent.getCharm().getName());
    assertNotNull(item.getComponent(PhysicsComponent.class));
    assertNotNull(item.getComponent(HitboxComponent.class));
    assertNotNull(item.getComponent(TextureRenderComponent.class));
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
