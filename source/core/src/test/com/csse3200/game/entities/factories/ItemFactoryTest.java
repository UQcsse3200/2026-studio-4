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
import com.csse3200.game.components.items.ItemSpinComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.CurrencyItem;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.WeaponItem;
import com.csse3200.game.items.consumables.InstantHealingPotion;
import com.csse3200.game.items.consumables.ShieldPotion;
import com.csse3200.game.items.consumables.SpeedPotion;
import com.csse3200.game.items.consumables.StrengthPotion;
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
  @BeforeEach
  void setup() {
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerPhysicsService(new PhysicsService());

    ResourceService resourceService = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    for (String itemId : ItemCatalog.ids()) {
      when(resourceService.getAsset(ItemCatalog.create(itemId, 1).getTexture(), Texture.class))
          .thenReturn(texture);
    }
    when(texture.getWidth()).thenReturn(1270);
    when(texture.getHeight()).thenReturn(1239);
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void shouldCreateSharedItemWithCorrectComponents() {
    Entity itemEntity =
        ItemFactory.createItem(ItemCatalog.create(ItemIds.STRENGTH_CHARM, 1), new Vector2());

    assertNotNull(itemEntity.getComponent(ItemComponent.class));
    assertNotNull(itemEntity.getComponent(RotatingTextureRenderComponent.class));
    assertNotNull(itemEntity.getComponent(ItemSpinComponent.class));
    assertNotNull(itemEntity.getComponent(HitboxComponent.class));
    assertNotNull(itemEntity.getComponent(PhysicsComponent.class));
    assertEquals(PhysicsLayer.ITEM, itemEntity.getComponent(HitboxComponent.class).getLayer());
  }

  @Test
  void shouldCreateEveryRegisteredItemId() {
    for (String expectedType : ItemCatalog.ids()) {
      Entity created = ItemFactory.createItem(ItemCatalog.create(expectedType, 1), Vector2.Zero);

      assertEquals(expectedType, created.getComponent(ItemComponent.class).getItemId());
      assertEquals(PhysicsLayer.ITEM, created.getComponent(HitboxComponent.class).getLayer());
    }
  }

  @Test
  void shouldCreateAnUnregisteredItemWithoutInspectingItsSubtype() {
    WeaponItem weapon = WeaponItem.createWeaponItem(WeaponItem.WeaponType.SWORD);
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(1);
    when(texture.getHeight()).thenReturn(1);
    when(ServiceLocator.getResourceService().getAsset(weapon.getTexture(), Texture.class))
        .thenReturn(texture);

    Entity created = ItemFactory.createItem(weapon, new Vector2());

    assertEquals(weapon, created.getComponent(ItemComponent.class).getItem());
  }

  @Test
  void shouldPreserveCallerSelectedGoldQuantity() {
    Entity gold =
        ItemFactory.createItem(ItemCatalog.create(ItemIds.GOLD_COIN, 25), new Vector2(2f, 4f));

    assertEquals(ItemIds.GOLD_COIN, gold.getComponent(ItemComponent.class).getItemId());
    assertEquals(25, gold.getComponent(ItemComponent.class).getQuantity());
    assertEquals(CurrencyItem.class, gold.getComponent(ItemComponent.class).getItem().getClass());
  }

  @Test
  void shouldCreateConsumableThroughTheSameRegistry() {
    Entity potion =
        ItemFactory.createItem(ItemCatalog.create(ItemIds.HEALTH_POTION, 1), new Vector2());
    assertEquals(
        InstantHealingPotion.class, potion.getComponent(ItemComponent.class).getItem().getClass());
    assertEquals(ShieldPotion.class, ItemCatalog.create(ItemIds.SHIELD, 1).getClass());
    assertEquals(SpeedPotion.class, ItemCatalog.create(ItemIds.SPEED_POTION, 1).getClass());
    assertEquals(StrengthPotion.class, ItemCatalog.create(ItemIds.STRENGTH_POTION, 1).getClass());
  }

  @Test
  void shouldCreateIndependentCharmItemsWhenRequestedTwice() {
    Entity first =
        ItemFactory.createItem(ItemCatalog.create(ItemIds.SPEED_CHARM, 1), new Vector2(2f, 4f));
    Entity second =
        ItemFactory.createItem(ItemCatalog.create(ItemIds.SPEED_CHARM, 1), new Vector2(2f, 4f));

    assertNotSame(first, second);
    assertNotSame(
        first.getComponent(ItemComponent.class).getItem(),
        second.getComponent(ItemComponent.class).getItem());
    assertEquals(ItemIds.SPEED_CHARM, itemTypeOf(first));
    assertEquals(ItemIds.SPEED_CHARM, itemTypeOf(second));
    assertEquals(1, first.getComponent(ItemComponent.class).getQuantity());
    assertNotNull(first.getComponent(ItemSpinComponent.class));
  }

  @Test
  void shouldCreateMultipleAndRepeatedConsumablesSelectedByCaller() {
    Vector2 position = new Vector2(3f, 5f);
    List<ItemDropSpec> selectedDrops =
        List.of(
            ItemDropSpec.single(ItemIds.HEALTH_POTION),
            ItemDropSpec.single(ItemIds.SPEED_POTION),
            new ItemDropSpec(ItemIds.HEALTH_POTION, 2));

    List<Entity> drops =
        selectedDrops.stream()
            .map(
                spec ->
                    ItemFactory.createItem(
                        ItemCatalog.create(spec.itemId(), spec.quantity()), position))
            .toList();

    assertEquals(3, drops.size());
    assertEquals(ItemIds.HEALTH_POTION, itemTypeOf(drops.get(0)));
    assertEquals(ItemIds.SPEED_POTION, itemTypeOf(drops.get(1)));
    assertEquals(ItemIds.HEALTH_POTION, itemTypeOf(drops.get(2)));
    assertEquals(2, drops.get(2).getComponent(ItemComponent.class).getQuantity());
    drops.forEach(drop -> assertEquals(position, drop.getPosition()));
    assertNotSame(drops.get(0), drops.get(2));
  }

  @Test
  void shouldRejectNullItemOrPosition() {
    Vector2 validPosition = new Vector2(1f, 2f);
    assertThrows(NullPointerException.class, () -> ItemFactory.createItem(null, validPosition));
    assertThrows(
        NullPointerException.class,
        () -> ItemFactory.createItem(ItemCatalog.create(ItemIds.GOLD_COIN, 1), null));
  }

  private static String itemTypeOf(Entity entity) {
    return entity.getComponent(ItemComponent.class).getItemId();
  }

  @Test
  void shouldCreateAtCorrectPostion() {
    Entity itemEntity =
        ItemFactory.createItem(ItemCatalog.create(ItemIds.STRENGTH_CHARM, 1), new Vector2(2, 2));
    assertEquals(new Vector2(2, 2), itemEntity.getPosition());
  }
}
