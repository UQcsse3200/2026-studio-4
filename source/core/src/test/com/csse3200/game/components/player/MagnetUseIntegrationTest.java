package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.ItemFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.LootTable;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Exercises Sumith's magnet with Jeremy's world drops and the real entity/physics lifecycle. */
@ExtendWith(GameExtension.class)
class MagnetUseIntegrationTest {
  private final AtomicLong now = new AtomicLong();
  private EntityService entities;
  private PhysicsService physics;
  private Entity player;
  private InventoryComponent inventory;
  private ConsumableEffectComponent consumables;
  private KeyboardPlayerInputComponent input;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
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
    consumables = new ConsumableEffectComponent();
    input = new KeyboardPlayerInputComponent();
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(inventory)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(consumables)
            .addComponent(new ConsumableSelectionComponent())
            .addComponent(input);
    entities.register(player);
  }

  @AfterEach
  void tearDown() {
    for (Entity entity : new Array<>(entities.getEntities())) entity.dispose();
    physics.getPhysics().dispose();
  }

  @Test
  void magnetTakesControlOfFountainDropWithoutOutwardPhysicsDrift() {
    LootTable loot = new LootTable();
    loot.rolls = 1;
    loot.entries = new LootTable.Entry[] {new LootTable.Entry(ItemIds.GOLD_COIN, 1, 3, 3)};
    Vector2 origin = player.getCenterPosition().add(3f, 0f);
    Entity gold = new ItemFactory(loot, new Random(7)).createEnemyDrops("test", origin).get(0);
    entities.register(gold);
    inventory.addConsumable(ItemIds.MAGNET_POTION);
    input.keyDown(Keys.Q);

    entities.update();
    assertEquals(2.7f, gold.getCenterPosition().dst(player.getCenterPosition()), 0.001f);
    Vector2 pulledPosition = gold.getPosition();
    physics.getPhysics().update();
    gold.earlyUpdate();

    assertEquals(pulledPosition.x, gold.getPosition().x, 0.001f);
    assertEquals(pulledPosition.y, gold.getPosition().y, 0.001f);
  }

  @Test
  void realFactoryDropsAreCollectedOnceAndUnregisteredAfterTheUpdate() {
    Entity gold = drop(ItemIds.GOLD_COIN, 7, 3f);
    Entity burn = drop(ItemIds.BURN_VIAL, 2, 2f);
    Entity charm = drop(ItemIds.STRENGTH_CHARM, 1, 1f);
    Entity farGold = drop(ItemIds.GOLD_COIN, 9, 8f);
    Vector2 charmPosition = charm.getPosition();
    Vector2 farPosition = farGold.getPosition();
    inventory.addConsumable(ItemIds.MAGNET_POTION);
    input.keyDown(Keys.Q);

    for (int i = 0; i < 30; i++) {
      physics.getPhysics().update();
      entities.update();
      now.addAndGet(50);
    }

    assertEquals(7, inventory.getGold());
    assertEquals(2, inventory.getConsumableCount(ItemIds.BURN_VIAL));
    assertFalse(entities.getEntities().contains(gold, true));
    assertFalse(entities.getEntities().contains(burn, true));
    assertEquals(charmPosition, charm.getPosition());
    assertEquals(farPosition, farGold.getPosition());
  }

  @Test
  void fourthSlotUsesMagnetAndRefreshesOneEffectRatherThanDoublingPull() {
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    inventory.addConsumable(ItemIds.SHIELD);
    inventory.addConsumable(ItemIds.BURN_VIAL);
    inventory.addConsumable(ItemIds.MAGNET_POTION, 2);
    for (int i = 0; i < 3; i++) input.keyDown(Keys.TAB);
    input.keyDown(Keys.Q);
    now.set(5000);
    input.keyDown(Keys.Q);
    assertEquals(10000, consumables.getRemainingMs(ItemIds.MAGNET_POTION));
    assertEquals(0, inventory.getConsumableCount(ItemIds.MAGNET_POTION));
    Entity gold = drop(ItemIds.GOLD_COIN, 1, 3f);
    entities.update();
    assertEquals(2.7f, gold.getCenterPosition().dst(player.getCenterPosition()), 0.001f);
    assertEquals(1, inventory.getConsumableCount(ItemIds.BURN_VIAL));
    now.set(15000);
    Vector2 position = gold.getPosition();
    entities.update();
    assertEquals(position, gold.getPosition());
    assertNull(player.getComponent(StatusEffectsControllerComponent.class).getGlow());
  }

  @Test
  void fifthTypeStaysInBackpackUntilEquippedAndUsesThroughTabQ() {
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    inventory.addConsumable(ItemIds.SHIELD);
    inventory.addConsumable(ItemIds.SPEED_POTION);
    inventory.addConsumable(ItemIds.BURN_VIAL);
    ItemCatalog.create(ItemIds.MAGNET_POTION, 2).pickUp(player);
    assertEquals(ItemIds.BURN_VIAL, inventory.getConsumableSlot(3));
    assertEquals(2, inventory.getConsumableCount(ItemIds.MAGNET_POTION));
    inventory.equipConsumable(ItemIds.MAGNET_POTION, 3);
    for (int i = 0; i < 3; i++) input.keyDown(Keys.TAB);
    input.keyDown(Keys.Q);
    assertEquals(1, inventory.getConsumableCount(ItemIds.MAGNET_POTION));
    assertEquals(10000, consumables.getRemainingMs(ItemIds.MAGNET_POTION));
    assertEquals(1, inventory.getConsumableCount(ItemIds.BURN_VIAL));
  }

  @Test
  void clearingEffectsStopsPullAndKeepsUncollectedWorldItems() {
    Entity gold = drop(ItemIds.GOLD_COIN, 4, 3f);
    inventory.addConsumable(ItemIds.MAGNET_POTION);
    input.keyDown(Keys.Q);
    player.getComponent(StatusEffectsControllerComponent.class).clearStatusEffects();
    Vector2 position = gold.getPosition();
    entities.update();
    assertEquals(position, gold.getPosition());
    assertEquals(0, inventory.getGold());
    assertEquals(0, consumables.getRemainingMs(ItemIds.MAGNET_POTION));
  }

  private Entity drop(String id, int quantity, float distance) {
    Vector2 position = player.getCenterPosition().add(distance - 0.5f, -0.5f);
    Entity entity = ItemFactory.createItem(ItemCatalog.create(id, quantity), position);
    entities.register(entity);
    return entity;
  }
}
