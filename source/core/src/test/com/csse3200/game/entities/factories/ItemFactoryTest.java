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
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
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
    for (ItemType itemType : ItemType.values()) {
      when(resourceService.getAsset(itemType.getTexturePath(), Texture.class)).thenReturn(texture);
    }
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
  void shouldCreateEverySupportedItemType() {
    for (ItemType expectedType : ItemType.values()) {
      Entity item = ItemFactory.createDrop(expectedType, Vector2.Zero);

      assertNotNull(item);
      assertEquals(expectedType, item.getComponent(ItemComponent.class).getItemType());
      assertNotNull(item.getComponent(PhysicsComponent.class));
      assertNotNull(item.getComponent(HitboxComponent.class));
      assertNotNull(item.getComponent(TextureRenderComponent.class));
      assertEquals(PhysicsLayer.ITEM, item.getComponent(HitboxComponent.class).getLayer());
    }
  }

  @Test
  void shouldPreserveCallerSelectedGoldQuantity() {
    Entity gold =
        ItemFactory.createDrop(new ItemDropSpec(ItemType.GOLD_COIN, 25), new Vector2(2f, 4f));

    assertEquals(ItemType.GOLD_COIN, gold.getComponent(ItemComponent.class).getItemType());
    assertEquals(25, gold.getComponent(ItemComponent.class).getQuantity());
  }

  @Test
  void shouldCreateMultipleAndRepeatedConsumablesSelectedByCaller() {
    Vector2 position = new Vector2(3f, 5f);
    List<ItemDropSpec> selectedDrops =
        List.of(
            ItemDropSpec.single(ItemType.HEALTH_POTION),
            ItemDropSpec.single(ItemType.SPEED_POTION),
            new ItemDropSpec(ItemType.HEALTH_POTION, 2));

    List<Entity> drops = ItemFactory.createDrops(selectedDrops, position);

    assertEquals(3, drops.size());
    assertEquals(ItemType.HEALTH_POTION, itemTypeOf(drops.get(0)));
    assertEquals(ItemType.SPEED_POTION, itemTypeOf(drops.get(1)));
    assertEquals(ItemType.HEALTH_POTION, itemTypeOf(drops.get(2)));
    assertEquals(2, drops.get(2).getComponent(ItemComponent.class).getQuantity());
    drops.forEach(drop -> assertEquals(position, drop.getPosition()));
    assertNotSame(drops.get(0), drops.get(2));
  }

  @Test
  void shouldRejectInvalidDropRequest() {
    Vector2 validPosition = new Vector2(1f, 2f);
    assertThrows(
        NullPointerException.class, () -> ItemFactory.createDrop((ItemType) null, validPosition));
    assertThrows(
        NullPointerException.class, () -> ItemFactory.createDrop(ItemType.STRENGTH_CHARM, null));
    assertThrows(
        NullPointerException.class,
        () -> ItemFactory.createDrop((ItemDropSpec) null, validPosition));
    assertThrows(NullPointerException.class, () -> ItemFactory.createDrops(null, validPosition));
    assertThrows(
        NullPointerException.class,
        () -> ItemFactory.createDrops(List.of(ItemDropSpec.single(ItemType.GOLD_COIN)), null));
  }

  @Test
  void shouldCreateStrengthCharm() {
    Entity item = ItemFactory.createStrengthCharm();

    ItemComponent itemComponent = item.getComponent(ItemComponent.class);
    assertEquals(PhysicsLayer.ITEM, (short) (1 << 5));
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

  private static ItemType itemTypeOf(Entity entity) {
    return entity.getComponent(ItemComponent.class).getItemType();
  }
}
