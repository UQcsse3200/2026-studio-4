package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.maingame.InteractionPromptDisplay;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.statuseffects.MagnetEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Verifies shared collection guards before deferred disposal has removed a world item. */
@ExtendWith(GameExtension.class)
class ItemPickupComponentTest {
  private EntityService entities;
  private PhysicsService physics;
  private Entity player;
  private InventoryComponent inventory;
  private ItemPickupComponent pickup;
  private MagnetEffect magnet;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.05f);
    ServiceLocator.registerTimeSource(time);
    entities = spy(new EntityService());
    ServiceLocator.registerEntityService(entities);
    physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    inventory = new InventoryComponent(0);
    pickup = new ItemPickupComponent();
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(inventory)
            .addComponent(pickup);
    entities.register(player);
    magnet = new MagnetEffect(time, 1000L, player, 5f, 6f, 0.5f);
  }

  @AfterEach
  void tearDown() {
    for (Entity entity : new Array<>(entities.getEntities())) {
      entity.dispose();
    }
    physics.getPhysics().dispose();
  }

  @Test
  void ordinaryPickupAfterMagnetInSameFrameDoesNotGrantTheRewardAgain() {
    Entity coin = drop(ItemIds.GOLD_COIN, 7);
    enterPickupRange(coin);

    magnet.update();
    assertTrue(entities.getEntities().contains(coin, true));
    assertNull(pickup.getPickupPrompt());
    player.getEvents().trigger("itemPickup");

    assertEquals(7, inventory.getGold());
    verify(entities, times(1)).scheduleDisposal(coin);
    entities.update();
    assertFalse(entities.getEntities().contains(coin, true));
  }

  @Test
  void magnetAfterOrdinaryPickupInSameFrameDoesNotGrantTheRewardAgain() {
    Entity potion = drop(ItemIds.HEALTH_POTION, 2);
    enterPickupRange(potion);

    player.getEvents().trigger("itemPickup");
    assertTrue(entities.getEntities().contains(potion, true));
    magnet.update();
    player.getEvents().trigger("itemPickup");

    assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertNull(pickup.getPickupPrompt());
    verify(entities, times(1)).scheduleDisposal(potion);
  }

  @Test
  void pickupSkipsCollectedOverlapAndCollectsTheNextAvailableItem() {
    Entity first = drop(ItemIds.GOLD_COIN, 7);
    Entity second = drop(ItemIds.HEALTH_POTION, 2);
    enterPickupRange(first);
    enterPickupRange(second);
    first.getComponent(ItemComponent.class).tryPickUp(player);
    entities.scheduleDisposal(first);

    player.getEvents().trigger("itemPickup");

    assertEquals(7, inventory.getGold());
    assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertNull(pickup.getPickupPrompt());
  }

  @Test
  void magnetPotionStillLeavesCharmsForManualPickup() {
    Entity charm = drop(ItemIds.SPEED_CHARM, 1);
    enterPickupRange(charm);

    magnet.update();

    assertFalse(charm.getComponent(ItemComponent.class).isCollected());
    assertEquals(0, inventory.getCharmCount());
    assertNotNull(pickup.getPickupPrompt());
  }

  @Test
  void showsThePickupPromptWhileTheDropIsInRange() {
    InteractionPromptDisplay display = mock(InteractionPromptDisplay.class);
    Entity coin = drop(ItemIds.GOLD_COIN, 7);
    enterPickupRange(coin);

    pickup.refreshPickupPrompt(display);

    verify(display).setPrompt("Press E — Pick up Gold Coin");
  }

  @Test
  void hidesThePickupPromptAfterThePlayerWalksAway() {
    InteractionPromptDisplay display = mock(InteractionPromptDisplay.class);
    Entity coin = drop(ItemIds.GOLD_COIN, 7);
    enterPickupRange(coin);
    pickup.refreshPickupPrompt(display);
    leavePickupRange(coin);

    pickup.refreshPickupPrompt(display);

    verify(display).clearPrompt();
  }

  @Test
  void leavesAnotherPromptAloneWhenNoDropIsInRange() {
    InteractionPromptDisplay display = mock(InteractionPromptDisplay.class);

    pickup.refreshPickupPrompt(display);

    verify(display, never()).setPrompt(any());
    verify(display, never()).clearPrompt();
  }

  @Test
  void disposedItemDoesNotRemainAvailableThroughStaleCollisionOverlap() {
    Entity coin = drop(ItemIds.GOLD_COIN, 7);
    enterPickupRange(coin);

    coin.dispose();
    player.getEvents().trigger("itemPickup");

    assertEquals(0, inventory.getGold());
    assertNull(pickup.getPickupPrompt());
  }

  private Entity drop(String id, int quantity) {
    Entity item =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new ItemComponent(new ItemDropSpec(id, quantity)));
    item.setPosition(player.getPosition());
    entities.register(item);
    return item;
  }

  private void enterPickupRange(Entity item) {
    player
        .getEvents()
        .trigger(
            "collisionStart",
            player.getComponent(HitboxComponent.class).getFixture(),
            item.getComponent(HitboxComponent.class).getFixture());
    assertNotNull(pickup.getPickupPrompt());
  }

  private void leavePickupRange(Entity item) {
    player
        .getEvents()
        .trigger(
            "collisionEnd",
            player.getComponent(HitboxComponent.class).getFixture(),
            item.getComponent(HitboxComponent.class).getFixture());
  }
}
