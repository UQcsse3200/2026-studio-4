package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.items.ItemPickupComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checks the pickup, inventory and use flow independently of any HUD implementation. */
@ExtendWith(GameExtension.class)
class ItemPickupUseIntegrationTest {
  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerTimeSource(new GameTime());
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
  }

  @Test
  void pickedUpPotionIsStoredAndUsedOnceByKeyboard() {
    Entity player = createPlayer();
    player.getComponent(CombatStatsComponent.class).setHealth(50);

    pickUp(player, createItem(new ItemComponent(new ItemDropSpec(ItemType.HEALTH_POTION, 2))));
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    assertEquals(2, inventory.getConsumableCount(ItemType.HEALTH_POTION));

    player.getComponent(KeyboardPlayerInputComponent.class).keyDown(Keys.NUM_7);
    assertEquals(75, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(1, inventory.getConsumableCount(ItemType.HEALTH_POTION));
  }

  @Test
  void pickedUpCharmIsStoredAndItsEffectCanBeRemoved() {
    Entity player = createPlayer();
    StrengthCharm charm = new StrengthCharm();

    pickUp(player, createItem(new ItemComponent(charm)));
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    assertTrue(inventory.hasCharm(charm));
    assertEquals(18, stats.getBaseAttack());

    charm.drop(player);
    assertEquals(0, inventory.getCharmCount());
    assertEquals(8, stats.getBaseAttack());
  }

  private Entity createPlayer() {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(100, 8))
            .addComponent(new InventoryComponent(0))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new ConsumableEffectComponent())
            .addComponent(new ConsumableLoadoutComponent())
            .addComponent(new KeyboardPlayerInputComponent())
            .addComponent(new ItemPickupComponent());
    player.create();
    return player;
  }

  private Entity createItem(ItemComponent itemComponent) {
    Entity item =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(itemComponent);
    item.create();
    return item;
  }

  private void pickUp(Entity player, Entity item) {
    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    Fixture itemFixture = item.getComponent(HitboxComponent.class).getFixture();
    player.getEvents().trigger("collisionStart", playerFixture, itemFixture);
    player.getEvents().trigger("itemPickup");
  }
}
