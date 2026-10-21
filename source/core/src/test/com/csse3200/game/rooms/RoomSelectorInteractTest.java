package com.csse3200.game.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.items.ItemPickupComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemDropSpec;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** A drop in pickup range consumes E, so the same press does not also use a door. */
@ExtendWith(GameExtension.class)
class RoomSelectorInteractTest {
  private EntityService entities;
  private PhysicsService physics;
  private Entity player;
  private Entity room;
  private RoomSelector selector;
  private int exitRequests;
  private int npcInteractions;

  @BeforeEach
  void setUp() {
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);

    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new ItemPickupComponent());
    entities.register(player);

    room = new Entity();
    room.getEvents().addListener("requestExit", (Vector2 position) -> exitRequests++);
    room.getEvents().addListener("npcInteract", (Entity ignored) -> npcInteractions++);
    selector = new RoomSelector(room, player, null, null);
  }

  @AfterEach
  void tearDown() {
    player.dispose();
    physics.getPhysics().dispose();
  }

  @Test
  void asksTheDoorWhenNoDropIsInRange() {
    selector.onPlayerInteract();

    assertEquals(1, exitRequests);
    assertEquals(1, npcInteractions);
  }

  @Test
  void leavesTheDoorAloneWhileADropIsInRange() {
    Entity coin = drop(ItemIds.GOLD_COIN);
    player
        .getEvents()
        .trigger(
            "collisionStart",
            player.getComponent(HitboxComponent.class).getFixture(),
            coin.getComponent(HitboxComponent.class).getFixture());

    selector.onPlayerInteract();

    assertEquals(0, exitRequests);
    assertEquals(1, npcInteractions);
  }

  private Entity drop(String id) {
    Entity item =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new ItemComponent(new ItemDropSpec(id, 1)));
    entities.register(item);
    return item;
  }
}
