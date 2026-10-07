package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;

/** Tests Aarash's item through the existing inventory, input and real burn controller. */
@ExtendWith(GameExtension.class)
class BurnVialUseIntegrationTest {
  private Entity player;
  private Entity visibleEnemy;
  private Entity offscreenEnemy;
  private InventoryComponent inventory;
  private ConsumableEffectComponent consumables;
  private KeyboardPlayerInputComponent input;
  private final AtomicLong now = new AtomicLong();
  private final AtomicInteger uses = new AtomicInteger();

  @BeforeEach
  void setUp() {
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    ServiceLocator.registerWorldCamera(new OrthographicCamera(20f, 10f));
    EntityService entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
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
    player.getComponent(StatusEffectsControllerComponent.class).create();
    consumables.create();
    player.getComponent(ConsumableSelectionComponent.class).create();
    player
        .getEvents()
        .addListener(ConsumableEffectComponent.USED, (String id) -> uses.incrementAndGet());
    visibleEnemy = enemyAt(2f);
    offscreenEnemy = enemyAt(11f);
    entities.getEntities().addAll(player, visibleEnemy, offscreenEnemy);
  }

  @Test
  void pickupTabAndQConsumeOneVialAndDamageOnlyTheVisibleEnemy() {
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    ItemCatalog.create(ItemIds.BURN_VIAL, 2).pickUp(player);
    assertEquals(ItemIds.BURN_VIAL, inventory.getConsumableSlot(1));
    try (MockedConstruction<GameTime> clocks = burnClocks()) {
      input.keyDown(Keys.TAB);
      input.keyDown(Keys.Q);
      assertEquals(1, inventory.getConsumableCount(ItemIds.BURN_VIAL));
      assertEquals(1, uses.get());
      assertEquals(1, clocks.constructed().size());
      now.set(1000);
      tickEnemies();
      assertEquals(100, health(visibleEnemy));
      now.set(1001);
      tickEnemies();
      assertEquals(98, health(visibleEnemy));
      assertEquals(100, health(offscreenEnemy));
      assertEquals(100, health(player));
    }
  }

  @Test
  void repeatedUsesReuseIndependentBurnStacksAndStopAfterExpiry() {
    inventory.addConsumable(ItemIds.BURN_VIAL, 2);
    try (MockedConstruction<GameTime> clocks = burnClocks()) {
      assertTrue(consumables.tryUse(ItemIds.BURN_VIAL));
      assertTrue(consumables.tryUse(ItemIds.BURN_VIAL));
      assertFalse(consumables.tryUse(ItemIds.BURN_VIAL));
      assertEquals(2, clocks.constructed().size());
      assertEquals(2, uses.get());
      assertNull(inventory.getConsumableSlot(0));
      // Exercise the existing Burning cadence, including its damage-before-expiry check.
      for (int tick = 1; tick <= 6; tick++) {
        now.set(tick * 1001L);
        tickEnemies();
        assertEquals(100 - tick * 4, health(visibleEnemy));
      }
      now.set(10000);
      tickEnemies();
      assertEquals(76, health(visibleEnemy));
      assertEquals(100, health(offscreenEnemy));
    }
  }

  @Test
  void unavailableWorldKeepsTheVialAndDoesNotPublishSuccess() {
    inventory.addConsumable(ItemIds.BURN_VIAL);
    ServiceLocator.registerWorldCamera(null);
    assertFalse(consumables.tryUse(ItemIds.BURN_VIAL));
    ServiceLocator.registerWorldCamera(new OrthographicCamera(20f, 10f));
    ServiceLocator.registerEntityService(null);
    assertFalse(consumables.tryUse(ItemIds.BURN_VIAL));
    assertEquals(1, inventory.getConsumableCount(ItemIds.BURN_VIAL));
    assertEquals(ItemIds.BURN_VIAL, inventory.getConsumableSlot(0));
    assertEquals(0, uses.get());
  }

  @Test
  void burnVialFitsTheFourthSlotAndTabStillWrapsAfterFourSlots() {
    inventory.addConsumable(ItemIds.HEALTH_POTION);
    inventory.addConsumable(ItemIds.SHIELD);
    inventory.addConsumable(ItemIds.SPEED_POTION);
    ItemCatalog.create(ItemIds.BURN_VIAL, 1).pickUp(player);
    for (int i = 0; i < 3; i++) input.keyDown(Keys.TAB);
    ConsumableSelectionComponent selection =
        player.getComponent(ConsumableSelectionComponent.class);
    assertEquals(ItemIds.BURN_VIAL, selection.getSelectedType());
    input.keyDown(Keys.TAB);
    assertEquals(0, selection.getSelectedIndex());
    assertEquals(ItemIds.HEALTH_POTION, selection.getSelectedType());
    assertEquals(4, InventoryComponent.CONSUMABLE_SLOT_COUNT);
  }

  private MockedConstruction<GameTime> burnClocks() {
    return mockConstruction(
        GameTime.class,
        (clock, context) -> {
          when(clock.getTime()).thenAnswer(invocation -> now.get());
          when(clock.getTimeSince(anyLong()))
              .thenAnswer(invocation -> now.get() - (Long) invocation.getArgument(0));
        });
  }

  private Entity enemyAt(float x) {
    Filter filter = new Filter();
    filter.categoryBits = PhysicsLayer.NPC;
    Fixture fixture = mock(Fixture.class);
    when(fixture.getFilterData()).thenReturn(filter);
    HitboxComponent hitbox = mock(HitboxComponent.class);
    when(hitbox.getFixture()).thenReturn(fixture);
    StatusEffectsControllerComponent effects = new StatusEffectsControllerComponent();
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 5))
            .addComponent(hitbox)
            .addComponent(effects);
    enemy.setPosition(x, 0f);
    effects.create();
    return enemy;
  }

  private void tickEnemies() {
    visibleEnemy.getComponent(StatusEffectsControllerComponent.class).update();
    offscreenEnemy.getComponent(StatusEffectsControllerComponent.class).update();
  }

  private int health(Entity entity) {
    return entity.getComponent(CombatStatsComponent.class).getHealth();
  }
}
