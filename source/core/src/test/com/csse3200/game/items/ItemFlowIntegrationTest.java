package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.items.CharmPickupComponent;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.player.CharmEffectComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.ItemFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemFlowIntegrationTest {
  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ResourceService resourceService = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(resourceService.getAsset("images/strength_charm_pixel.png", Texture.class))
        .thenReturn(texture);
    when(texture.getWidth()).thenReturn(1270);
    when(texture.getHeight()).thenReturn(1239);
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void shouldPickUpFactoryDropApplyBuffAndRestoreBaseAttackOnRemoval() {
    Entity player = createPlayer();
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    CombatStatsComponent combatStats = player.getComponent(CombatStatsComponent.class);
    Entity droppedItem = ItemFactory.createDrop(ItemType.STRENGTH_CHARM, Vector2.Zero);
    Charm droppedCharm = droppedItem.getComponent(ItemComponent.class).getCharm();
    droppedItem.create();

    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    Fixture itemFixture = droppedItem.getComponent(HitboxComponent.class).getFixture();
    player.getEvents().trigger("collisionStart", playerFixture, itemFixture);
    player.getEvents().trigger("interact");

    assertEquals(1, inventory.getCharmCount());
    assertSame(droppedCharm, inventory.getCharms().get(0));
    assertEquals(20, combatStats.getBaseAttack());

    inventory.removeCharm(droppedCharm);

    assertEquals(0, inventory.getCharmCount());
    assertEquals(10, combatStats.getBaseAttack());
  }

  private Entity createPlayer() {
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new InventoryComponent(0))
            .addComponent(new CharmEffectComponent())
            .addComponent(new CharmPickupComponent());
    player.create();
    return player;
  }
}
