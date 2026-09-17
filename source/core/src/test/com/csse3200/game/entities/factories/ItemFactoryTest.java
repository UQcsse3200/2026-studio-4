package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.items.ItemSpinComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.RotatingTextureRenderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemFactoryTest {
  private Item item;

  @BeforeEach
  void setup() {
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerPhysicsService(new PhysicsService());

    ResourceService resourceService = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    for (ItemType itemType : ItemType.values()) {
      when(resourceService.getAsset(itemType.getTexturePath(), Texture.class)).thenReturn(texture);
    }
    when(texture.getWidth()).thenReturn(1270);
    when(texture.getHeight()).thenReturn(1239);
    ServiceLocator.registerResourceService(resourceService);

    item = mock(Item.class);
    when(item.getTexture()).thenReturn(StrengthCharm.TEXTURE);
  }

  @Test
  void shouldCreateSharedItemWithCorrectComponents() {
    Entity itemEntity = ItemFactory.createItem(item);

    assertNotNull(itemEntity.getComponent(ItemComponent.class));
    assertNotNull(itemEntity.getComponent(RotatingTextureRenderComponent.class));
    assertNull(itemEntity.getComponent(ItemSpinComponent.class));
    assertNotNull(itemEntity.getComponent(HitboxComponent.class));
    assertNotNull(itemEntity.getComponent(PhysicsComponent.class));
    assertEquals(PhysicsLayer.ITEM, itemEntity.getComponent(HitboxComponent.class).getLayer());
  }

  @Test
  void shouldCreateEverySupportedItemType() {
    for (ItemType expectedType : ItemType.values()) {
      Entity created = ItemFactory.createDrop(expectedType, Vector2.Zero);

      assertEquals(expectedType, created.getComponent(ItemComponent.class).getItemType());
      assertEquals(PhysicsLayer.ITEM, created.getComponent(HitboxComponent.class).getLayer());
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

  private static ItemType itemTypeOf(Entity entity) {
    return entity.getComponent(ItemComponent.class).getItemType();
  }

  @Test
  void sharedPoolCreatesEveryRegisteredItemAsAFreshPositionedEntity() {
    ItemType[] types = {
      ItemType.STRENGTH_CHARM,
      ItemType.HEALTH_POTION,
      ItemType.SHIELD,
      ItemType.SPEED_POTION,
      ItemType.STRENGTH_POTION,
      ItemType.GOLD_COIN
    };
    java.util.random.RandomGenerator random = mock(java.util.random.RandomGenerator.class);
    for (int i = 0; i < types.length; i++) {
      when(random.nextInt(types.length)).thenReturn(i);
      Entity first = ItemFactory.createRandomDrop(new Vector2(3, 4), random);
      Entity second = ItemFactory.createRandomDrop(new Vector2(3, 4), random);
      assertEquals(types[i], itemTypeOf(first));
      assertEquals(
          types[i] == ItemType.GOLD_COIN ? 5 : 1,
          first.getComponent(ItemComponent.class).getQuantity());
      assertEquals(new Vector2(3, 4), first.getPosition());
      assertNotSame(first, second);
      assertNotSame(
          first.getComponent(ItemComponent.class).getItem(),
          second.getComponent(ItemComponent.class).getItem());
    }
  }

  @Test
  void shouldCreateAtCorrectPostion() {
    Entity itemEntity = ItemFactory.createDrop(new Vector2(2, 2));
    Entity randomItemEntity = ItemFactory.createRandomDrop(new Vector2(1, 2));

    assertEquals(itemEntity.getPosition(), new Vector2(2, 2));
    assertEquals(randomItemEntity.getPosition(), new Vector2(1, 2));
  }
}
