package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.Charm;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CharmPickupComponentTest {
  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
  }

  @Test
  void shouldPickUpNearbyCharmOnInteract() {
    Charm charm = new Charm("Strength Charm");
    Entity player = createPlayer();
    Entity itemEntity = createItemEntity(charm);

    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    Fixture itemFixture = itemEntity.getComponent(HitboxComponent.class).getFixture();

    player.getEvents().trigger("collisionStart", playerFixture, itemFixture);
    player.getEvents().trigger("interact");

    assertTrue(player.getComponent(InventoryComponent.class).getCharms().contains(charm));
  }

  @Test
  void shouldNotPickUpWithoutInteract() {
    Charm charm = new Charm("Strength Charm");
    Entity player = createPlayer();
    Entity itemEntity = createItemEntity(charm);

    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    Fixture itemFixture = itemEntity.getComponent(HitboxComponent.class).getFixture();

    // Just walking near the item should not pick it up on its own.
    player.getEvents().trigger("collisionStart", playerFixture, itemFixture);

    assertEquals(0, player.getComponent(InventoryComponent.class).getCharmCount());
  }

  @Test
  void shouldNotPickUpAfterLeavingRange() {
    Charm charm = new Charm("Strength Charm");
    Entity player = createPlayer();
    Entity itemEntity = createItemEntity(charm);

    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    Fixture itemFixture = itemEntity.getComponent(HitboxComponent.class).getFixture();

    player.getEvents().trigger("collisionStart", playerFixture, itemFixture);
    player.getEvents().trigger("collisionEnd", playerFixture, itemFixture);
    player.getEvents().trigger("interact");

    assertEquals(0, player.getComponent(InventoryComponent.class).getCharmCount());
  }

  @Test
  void shouldIgnoreNonItemLayerCollisions() {
    Entity player = createPlayer();
    Entity other =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.OBSTACLE));
    other.create();

    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    Fixture otherFixture = other.getComponent(HitboxComponent.class).getFixture();

    // Should not throw, and should not register anything to pick up.
    player.getEvents().trigger("collisionStart", playerFixture, otherFixture);
    player.getEvents().trigger("interact");

    assertEquals(0, player.getComponent(InventoryComponent.class).getCharmCount());
  }

  private Entity createPlayer() {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new InventoryComponent(0))
            .addComponent(new CharmPickupComponent());
    player.create();
    return player;
  }

  /**
   * Builds a world entity carrying an {@link ItemComponent}, the same way the Item Factory's output
   * is expected to look once the Room Team spawns it (physics body + an ITEM-layer hitbox).
   */
  private Entity createItemEntity(Charm charm) {
    Entity itemEntity =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new ItemComponent(charm));
    itemEntity.create();
    return itemEntity;
  }
}
