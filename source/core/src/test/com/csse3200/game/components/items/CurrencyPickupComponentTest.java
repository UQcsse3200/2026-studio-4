package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.Team5CombatHudDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CurrencyPickupComponentTest {
  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
  }

  @Test
  void shouldAddDroppedQuantityToInventoryAndHud() {
    Entity player = createPlayer();
    Entity coin = createItem(new ItemDropSpec(ItemType.GOLD_COIN, 25));
    overlap(player, coin);

    player.getEvents().trigger("itemPickup");

    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    assertEquals(25, inventory.getGold());

    Team5CombatHudDisplay hud = spy(new Team5CombatHudDisplay());
    hud.setEntity(player);
    hud.update();
    verify(hud).updateGold(25);
  }

  @Test
  void shouldRequireItemPickupWhileInRange() {
    Entity player = createPlayer();
    Entity coin = createItem(new ItemDropSpec(ItemType.GOLD_COIN, 25));
    overlap(player, coin);

    assertEquals(0, player.getComponent(InventoryComponent.class).getGold());

    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    Fixture coinFixture = coin.getComponent(HitboxComponent.class).getFixture();
    player.getEvents().trigger("collisionEnd", playerFixture, coinFixture);
    player.getEvents().trigger("itemPickup");

    assertEquals(0, player.getComponent(InventoryComponent.class).getGold());
  }

  @Test
  void shouldIgnoreConsumables() {
    Entity player = createPlayer();
    Entity potion = createItem(ItemDropSpec.single(ItemType.HEALTH_POTION));
    overlap(player, potion);

    player.getEvents().trigger("itemPickup");

    assertEquals(0, player.getComponent(InventoryComponent.class).getGold());
    assertEquals(ItemType.HEALTH_POTION, potion.getComponent(ItemComponent.class).getItemType());
  }

  private static Entity createPlayer() {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new InventoryComponent(0))
            .addComponent(new CurrencyPickupComponent());
    player.create();
    return player;
  }

  private static Entity createItem(ItemDropSpec dropSpec) {
    Entity item =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new ItemComponent(dropSpec));
    item.create();
    return item;
  }

  private static void overlap(Entity player, Entity item) {
    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    Fixture itemFixture = item.getComponent(HitboxComponent.class).getFixture();
    player.getEvents().trigger("collisionStart", playerFixture, itemFixture);
  }
}
