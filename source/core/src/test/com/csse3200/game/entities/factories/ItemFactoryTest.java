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
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
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
    for (ItemType itemType : ItemType.values()) {
      when(resourceService.getAsset(itemType.getTexturePath(), Texture.class)).thenReturn(texture);
    }
    when(texture.getWidth()).thenReturn(1270);
    when(texture.getHeight()).thenReturn(1239);
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void shouldCreateSharedItemWithCorrectComponents() {
    Entity itemEntity =
        ItemFactory.createDrops(ItemType.STRENGTH_CHARM.createItem(1), 1, new Vector2()).get(0);

    assertNotNull(itemEntity.getComponent(ItemComponent.class));
    assertNotNull(itemEntity.getComponent(RotatingTextureRenderComponent.class));
    assertNotNull(itemEntity.getComponent(ItemSpinComponent.class));
    assertNotNull(itemEntity.getComponent(HitboxComponent.class));
    assertNotNull(itemEntity.getComponent(PhysicsComponent.class));
    assertEquals(PhysicsLayer.ITEM, itemEntity.getComponent(HitboxComponent.class).getLayer());
  }

  @Test
  void shouldCreateEverySupportedItemType() {
    for (ItemType expectedType : ItemType.values()) {
      Entity created = ItemFactory.createDrops(expectedType.createItem(1), 1, Vector2.Zero).get(0);

      assertEquals(expectedType, created.getComponent(ItemComponent.class).getItemType());
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

    Entity created = ItemFactory.createDrops(weapon, 1, new Vector2()).get(0);

    assertEquals(weapon, created.getComponent(ItemComponent.class).getItem());
    assertThrows(
        IllegalArgumentException.class, () -> ItemFactory.createDrops(weapon, 2, new Vector2()));
  }

  @Test
  void shouldPreserveCallerSelectedGoldQuantity() {
    Entity gold =
        ItemFactory.createDrops(ItemType.GOLD_COIN.createItem(1), 25, new Vector2(2f, 4f)).get(0);

    assertEquals(ItemType.GOLD_COIN, gold.getComponent(ItemComponent.class).getItemType());
    assertEquals(25, gold.getComponent(ItemComponent.class).getQuantity());
    assertEquals(CurrencyItem.class, gold.getComponent(ItemComponent.class).getItem().getClass());
  }

  @Test
  void shouldCreateConsumableThroughTheSameRegistry() {
    Entity potion =
        ItemFactory.createDrops(ItemType.HEALTH_POTION.createItem(1), 1, new Vector2()).get(0);
    assertEquals(
        InstantHealingPotion.class, potion.getComponent(ItemComponent.class).getItem().getClass());
    assertEquals(ShieldPotion.class, ItemType.SHIELD.createItem(1).getClass());
    assertEquals(SpeedPotion.class, ItemType.SPEED_POTION.createItem(1).getClass());
    assertEquals(StrengthPotion.class, ItemType.STRENGTH_POTION.createItem(1).getClass());
  }

  @Test
  void shouldCreateIndependentCharmEntitiesForQuantity() {
    List<Entity> charms =
        ItemFactory.createDrops(ItemType.SPEED_CHARM.createItem(1), 2, new Vector2(2f, 4f));

    assertEquals(2, charms.size());
    assertNotSame(charms.get(0), charms.get(1));
    assertEquals(ItemType.SPEED_CHARM, itemTypeOf(charms.get(0)));
    assertEquals(ItemType.SPEED_CHARM, itemTypeOf(charms.get(1)));
    assertEquals(1, charms.get(0).getComponent(ItemComponent.class).getQuantity());
    assertNotNull(charms.get(0).getComponent(ItemSpinComponent.class));
  }

  @Test
  void shouldCreateMultipleAndRepeatedConsumablesSelectedByCaller() {
    Vector2 position = new Vector2(3f, 5f);
    List<ItemDropSpec> selectedDrops =
        List.of(
            ItemDropSpec.single(ItemType.HEALTH_POTION),
            ItemDropSpec.single(ItemType.SPEED_POTION),
            new ItemDropSpec(ItemType.HEALTH_POTION, 2));

    List<Entity> drops =
        selectedDrops.stream()
            .flatMap(
                spec ->
                    ItemFactory.createDrops(
                        spec.itemType().createItem(1), spec.quantity(), position)
                        .stream())
            .toList();

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
    assertThrows(NullPointerException.class, () -> ItemFactory.createDrops(null, 1, validPosition));
    assertThrows(
        NullPointerException.class,
        () -> ItemFactory.createDrops(ItemType.GOLD_COIN.createItem(1), 1, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> ItemFactory.createDrops(ItemType.GOLD_COIN.createItem(1), 0, validPosition));
  }

  private static ItemType itemTypeOf(Entity entity) {
    return entity.getComponent(ItemComponent.class).getItemType();
  }

  @Test
  void shouldCreateAtCorrectPostion() {
    Entity itemEntity =
        ItemFactory.createDrops(ItemType.STRENGTH_CHARM.createItem(1), 1, new Vector2(2, 2)).get(0);
    assertEquals(new Vector2(2, 2), itemEntity.getPosition());
  }
}
