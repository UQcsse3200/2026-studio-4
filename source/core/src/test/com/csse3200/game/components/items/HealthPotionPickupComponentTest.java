package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
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
class HealthPotionPickupComponentTest {
  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
  }

  @Test
  void shouldHealOnItemPickup() {
    Entity player = createPlayer(40);
    Entity potion = createItem(ItemDropSpec.single(ItemType.HEALTH_POTION));
    overlap(player, potion);

    player.getEvents().trigger("itemPickup");

    assertEquals(65, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldClampHealingToMaximumHealth() {
    Entity player = createPlayer(90);
    Entity potion = createItem(ItemDropSpec.single(ItemType.HEALTH_POTION));
    overlap(player, potion);

    player.getEvents().trigger("itemPickup");

    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldRequireItemPickupWhileInRange() {
    Entity player = createPlayer(40);
    Entity potion = createItem(ItemDropSpec.single(ItemType.HEALTH_POTION));
    overlap(player, potion);

    assertEquals(40, player.getComponent(CombatStatsComponent.class).getHealth());

    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    Fixture potionFixture = potion.getComponent(HitboxComponent.class).getFixture();
    player.getEvents().trigger("collisionEnd", playerFixture, potionFixture);
    player.getEvents().trigger("itemPickup");

    assertEquals(40, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldIgnoreOtherItemTypes() {
    Entity player = createPlayer(40);
    Entity coin = createItem(new ItemDropSpec(ItemType.GOLD_COIN, 25));
    overlap(player, coin);

    player.getEvents().trigger("itemPickup");

    assertEquals(40, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(ItemType.GOLD_COIN, coin.getComponent(ItemComponent.class).getItemType());
  }

  private static Entity createPlayer(int health) {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new HealthPotionPickupComponent());
    player.create();
    player.getComponent(CombatStatsComponent.class).setHealth(health);
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
