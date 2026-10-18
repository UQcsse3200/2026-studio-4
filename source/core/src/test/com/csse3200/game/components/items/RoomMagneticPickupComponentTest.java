package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.KeyboardPlayerInputComponent;
import com.csse3200.game.components.statuseffects.MagnetEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.ItemFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.Terminal;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Exercises the two-second snapshot with real drops, input, inventory and physics bodies. */
@ExtendWith(GameExtension.class)
class RoomMagneticPickupComponentTest {
  private final AtomicLong now = new AtomicLong();
  private EntityService entities;
  private PhysicsService physics;
  private Entity player;
  private InventoryComponent inventory;
  private KeyboardPlayerInputComponent input;
  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> now.get());
    when(time.getDeltaTime()).thenReturn(0.05f);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerInputService(new InputService());
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    RenderService render = new RenderService();
    render.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(render);
    ResourceService resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(64);
    when(texture.getHeight()).thenReturn(64);
    for (String id : ItemCatalog.ids()) {
      when(resources.getAsset(ItemCatalog.create(id, 1).getTexture(), Texture.class))
          .thenReturn(texture);
    }
    ServiceLocator.registerResourceService(resources);
    inventory = new InventoryComponent(0);
    input = new KeyboardPlayerInputComponent();
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(inventory)
            .addComponent(input)
            .addComponent(new RoomMagneticPickupComponent());
    entities.register(player);
  }

  @AfterEach
  void tearDown() {
    for (Entity entity : new Array<>(entities.getEntities())) {
      entity.dispose();
    }
    physics.getPhysics().dispose();
  }

  @Test
  void capturesAtTwoSecondsAndFinishesAfterReleaseWithoutTakingLaterDrops() {
    Entity early = drop(ItemIds.GOLD_COIN, 3, 12f);
    Vector2 initialPosition = early.getPosition();
    input.keyDown(Keys.E);
    now.set(1999);
    entities.update();
    assertEquals(initialPosition, early.getPosition());
    Entity justBeforeThreshold = drop(ItemIds.HEALTH_POTION, 2, 15f);

    now.set(2000);
    entities.update();
    input.keyUp(Keys.E);
    Entity later = drop(ItemIds.GOLD_COIN, 7, 9f);
    Vector2 laterPosition = later.getPosition();
    runFrames(80);

    assertEquals(3, inventory.getGold());
    assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    assertFalse(entities.getEntities().contains(early, true));
    assertFalse(entities.getEntities().contains(justBeforeThreshold, true));
    assertEquals(laterPosition, later.getPosition());
    assertTrue(entities.getEntities().contains(later, true));
  }

  @Test
  void continuingToHoldDoesNotCaptureNewDropsButAnotherHoldDoes() {
    drop(ItemIds.GOLD_COIN, 2, 5f);
    completeHold(false);
    Entity later = drop(ItemIds.GOLD_COIN, 7, 6f);
    runFrames(100);
    input.keyDown(Keys.E); // Repeat while the same key is still down.
    input.update();
    assertEquals(2, inventory.getGold());
    assertTrue(entities.getEntities().contains(later, true));
    input.keyUp(Keys.E);
    completeHold(true);
    runFrames(30);
    assertEquals(9, inventory.getGold());
  }

  @Test
  void itemsFollowMovingPlayerAndIncludeCharmsDuringCombat() {
    Entity enemy = new Entity().addComponent(new CombatStatsComponent(50, 5));
    entities.register(enemy);
    Entity charm = drop(ItemIds.STRENGTH_CHARM, 1, 20f);
    Vector2 initialPosition = charm.getPosition();
    completeHold(true);
    player.setPosition(2f, 7f);
    entities.update();
    assertNotEquals(initialPosition, charm.getPosition());
    assertTrue(charm.getPosition().y > initialPosition.y);
    assertEquals(0, inventory.getCharmCount());
    runFrames(100);
    assertEquals(1, inventory.getCharmCount());
    assertTrue(entities.getEntities().contains(enemy, true));
  }

  @Test
  void aSecondBatchDoesNotAbandonUnfinishedTargets() {
    drop(ItemIds.GOLD_COIN, 3, 45f);
    completeHold(true);
    drop(ItemIds.GOLD_COIN, 4, 30f);
    completeHold(true);
    runFrames(150);
    assertEquals(7, inventory.getGold());
  }

  @Test
  void roomChangeCancelsPendingHoldAndAlreadyCapturedItems() {
    Entity oldDrop = drop(ItemIds.GOLD_COIN, 5, 20f);
    completeHold(true);
    Vector2 oldPosition = oldDrop.getPosition();
    input.keyDown(Keys.E);
    now.addAndGet(1500);
    player.getEvents().trigger(RoomMagneticPickupComponent.RESET_EVENT);
    oldDrop.dispose();
    Entity newRoomDrop = drop(ItemIds.GOLD_COIN, 9, 10f);
    now.addAndGet(3000);
    input.keyUp(Keys.E);
    runFrames(100);
    assertEquals(0, inventory.getGold());
    assertEquals(oldPosition, oldDrop.getPosition());
    assertTrue(entities.getEntities().contains(newRoomDrop, true));
  }

  @Test
  void alreadyCollectedAndDisposedTargetsAreSkipped() {
    Entity collected = drop(ItemIds.GOLD_COIN, 4, 15f);
    Entity disposed = drop(ItemIds.GOLD_COIN, 8, 12f);
    completeHold(true);
    assertTrue(collected.getComponent(ItemComponent.class).tryPickUp(player));
    entities.scheduleDisposal(collected);
    disposed.dispose();
    runFrames(100);
    assertEquals(4, inventory.getGold());
  }

  @Test
  void roomMagnetAndPotionCannotRewardTheSameItemTwice() {
    drop(ItemIds.GOLD_COIN, 4, 0.1f);
    completeHold(true);
    MagnetEffect potion = new MagnetEffect(time, 10000, player, 4f, 6f, 0.3f);
    potion.update();
    entities.update();
    potion.update();
    assertEquals(4, inventory.getGold());
  }

  @Test
  void attractionOverridesOutwardDropVelocity() {
    Entity drop = drop(ItemIds.GOLD_COIN, 2, 10f);
    drop.getComponent(PhysicsComponent.class).getBody().setLinearVelocity(20f, 0f);
    completeHold(true);
    entities.update();
    assertEquals(
        Vector2.Zero, drop.getComponent(PhysicsComponent.class).getBody().getLinearVelocity());
    Vector2 pulled = drop.getPosition();
    physics.getPhysics().update();
    drop.earlyUpdate();
    assertEquals(pulled, drop.getPosition());
  }

  @Test
  void openingTerminalCancelsUnfinishedHoldButNotAnActiveBatch() {
    Entity gold = drop(ItemIds.GOLD_COIN, 2, 12f);
    input.keyDown(Keys.E);
    now.set(1500);
    Terminal terminal = new Terminal();
    terminal.setOpen();
    now.set(5000);
    input.keyUp(Keys.E);
    terminal.setClosed();
    runFrames(10);
    assertEquals(0, inventory.getGold());
    assertTrue(entities.getEntities().contains(gold, true));
    completeHold(true);
    terminal.setOpen();
    runFrames(50);
    assertEquals(2, inventory.getGold());
  }

  @Test
  void playerDeathCancelsActiveAttraction() {
    Entity gold = drop(ItemIds.GOLD_COIN, 2, 10f);
    completeHold(true);
    player.getComponent(CombatStatsComponent.class).setHealth(0);
    runFrames(100);
    assertEquals(0, inventory.getGold());
    assertTrue(entities.getEntities().contains(gold, true));
  }

  private void completeHold(boolean release) {
    input.keyDown(Keys.E);
    now.addAndGet(2000);
    input.update();
    if (release) {
      input.keyUp(Keys.E);
    }
  }

  private void runFrames(int count) {
    for (int i = 0; i < count; i++) {
      now.addAndGet(50);
      entities.update();
    }
  }

  private Entity drop(String id, int quantity, float distance) {
    Entity item =
        ItemFactory.createItem(
            ItemCatalog.create(id, quantity),
            player.getCenterPosition().add(distance - 0.5f, -0.5f));
    entities.register(item);
    return item;
  }
}
