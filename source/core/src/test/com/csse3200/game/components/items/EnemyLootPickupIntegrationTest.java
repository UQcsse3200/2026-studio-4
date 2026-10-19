package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.ItemFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.LootTable;
import com.csse3200.game.items.charms.Charm;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Exercises configured enemy rewards through the shared world-item and inventory interfaces. */
@ExtendWith(GameExtension.class)
class EnemyLootPickupIntegrationTest {
  private LootTable defaults;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
    RenderService render = new RenderService();
    render.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(render);
    ResourceService resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(32);
    when(texture.getHeight()).thenReturn(24);
    for (String id : ItemCatalog.ids()) {
      when(resources.getAsset(ItemCatalog.create(id, 1).getTexture(), Texture.class))
          .thenReturn(texture);
    }
    ServiceLocator.registerResourceService(resources);
    defaults = LootTable.defaultTable();
  }

  @Test
  void everyRegisteredRewardCanDropAndEnterTheRealInventory() {
    assertEquals(13, ItemCatalog.ids().size());
    for (String id : ItemCatalog.ids().stream().sorted().toList()) {
      Entity player = player();
      Entity drop = configuredDrop(id);
      Item item = drop.getComponent(ItemComponent.class).getItem();
      int quantity = item.getQuantity();
      assertNotNull(drop.getComponent(ItemDropAnimationComponent.class), id);
      assertEquals(0.75f, drop.getScale().y, 0.0001f, id);
      InventoryComponent inventory = player.getComponent(InventoryComponent.class);

      enterPickupRange(player, drop);
      assertEquals(0, inventory.getGold(), id);
      assertEquals(0, inventory.getCharmCount(), id);
      assertEquals(0, inventory.getConsumableCount(id), id);
      player.getEvents().trigger("itemPickup");

      if (item instanceof Charm charm) {
        assertTrue(inventory.hasCharm(charm), id);
        assertTrue(charm.isEquipped(), id);
      } else if (item instanceof ConsumableItem) {
        assertEquals(quantity, inventory.getConsumableCount(id), id);
      } else {
        assertEquals(ItemIds.GOLD_COIN, id);
        assertEquals(quantity, inventory.getGold());
      }
      assertNull(player.getComponent(ItemPickupComponent.class).getPickupPrompt(), id);
      // A repeated pickup request must not collect the same disposed world item again.
      player.getEvents().trigger("itemPickup");
      assertEquals(item instanceof Charm ? 1 : 0, inventory.getCharmCount(), id);
      assertEquals(
          item instanceof ConsumableItem ? quantity : 0, inventory.getConsumableCount(id), id);
      assertEquals(ItemIds.GOLD_COIN.equals(id) ? quantity : 0, inventory.getGold(), id);
      player.dispose();
    }
  }

  @Test
  void allCharmTypesRemainIndependentAndEquipmentTransitionsRestoreEffectiveStats() {
    for (String id :
        List.of(ItemIds.STRENGTH_CHARM, ItemIds.SPEED_CHARM, ItemIds.ATTACK_SPEED_CHARM)) {
      Entity player = player();
      InventoryComponent inventory = player.getComponent(InventoryComponent.class);
      Entity firstDrop = configuredDrop(id);
      Entity secondDrop = configuredDrop(id);
      Charm first = firstDrop.getComponent(ItemComponent.class).getCharm();
      Charm second = secondDrop.getComponent(ItemComponent.class).getCharm();
      assertNotSame(first, second, id);
      enterPickupRange(player, firstDrop);
      player.getEvents().trigger("itemPickup");
      assertStats(player, id, 1);
      enterPickupRange(player, secondDrop);
      player.getEvents().trigger("itemPickup");
      assertEquals(2, inventory.getCharmCount(), id);
      assertStats(player, id, 2);

      inventory.setCharmEquipped(first, false);
      assertFalse(first.isEquipped(), id);
      assertTrue(second.isEquipped(), id);
      assertStats(player, id, 1);
      inventory.setCharmEquipped(second, false);
      assertStats(player, id, 0);
      assertEquals(2, inventory.getCharmCount(), id);
      inventory.setCharmEquipped(first, true);
      inventory.setCharmEquipped(first, true);
      assertStats(player, id, 1);
      inventory.setCharmEquipped(first, false);
      inventory.setCharmEquipped(first, false);
      assertStats(player, id, 0);
      player.dispose();
    }
  }

  private Entity configuredDrop(String id) {
    LootTable table = defaults.forEnemy("FINAL_BOSS");
    int total = table.noDropWeight;
    int boundary = -1;
    for (LootTable.Entry entry : table.entries) {
      if (id.equals(entry.itemId)) boundary = total;
      total += entry.weight;
    }
    assertTrue(boundary >= 0, "Configured FINAL_BOSS pool is missing " + id);
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextInt(total)).thenReturn(boundary);
    // Quantity rolls default to zero, selecting the configured minimum quantity.
    List<Entity> drops =
        new ItemFactory(defaults, random).createEnemyDrops("FINAL_BOSS", new Vector2(3f, 5f));
    assertEquals(1, drops.size(), id);
    Entity drop = drops.get(0);
    assertEquals(id, drop.getComponent(ItemComponent.class).getItemId());
    drop.create();
    return drop;
  }

  private Entity player() {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(100, 20, 3f, 2f))
            .addComponent(new InventoryComponent(0))
            .addComponent(new ItemPickupComponent());
    player.create();
    return player;
  }

  private void enterPickupRange(Entity player, Entity drop) {
    var playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    var itemFixture = drop.getComponent(HitboxComponent.class).getFixture();
    assertEquals(PhysicsLayer.ITEM, itemFixture.getFilterData().categoryBits);
    player.getEvents().trigger("collisionStart", playerFixture, itemFixture);
    assertNotNull(player.getComponent(ItemPickupComponent.class).getPickupPrompt());
  }

  private void assertStats(Entity player, String id, int equippedCount) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    assertEquals(
        20 + (ItemIds.STRENGTH_CHARM.equals(id) ? 10 * equippedCount : 0),
        stats.getEffectiveBaseAttack(),
        id);
    assertEquals(
        3f + (ItemIds.SPEED_CHARM.equals(id) ? equippedCount : 0),
        stats.getEffectiveMovementSpeed(),
        0.0001f,
        id);
    assertEquals(
        2f + (ItemIds.ATTACK_SPEED_CHARM.equals(id) ? equippedCount : 0),
        stats.getEffectiveAttackSpeed(),
        0.0001f,
        id);
  }
}
