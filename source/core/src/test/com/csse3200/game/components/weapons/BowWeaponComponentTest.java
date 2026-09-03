package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class BowWeaponComponentTest {
  private EntityService entityService;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);

    // The arrow hitbox now carries a sprite, so it needs somewhere to load and register it.
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);

    ResourceService resourceService = new ResourceService();
    resourceService.loadTextures(new String[] {"images/weapons/throwing_knife.png"});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void shouldSpawnTravellingHitboxOnAttack() {
    BowWeaponComponent bow = new BowWeaponComponent();
    Entity wielder =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new WeaponStatsComponent(0.5f, 0.8f, 0f))
            .addComponent(bow);
    wielder.create();

    assertTrue(bow.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(captor.capture());
    Entity arrow = captor.getValue();

    assertNotNull(arrow.getComponent(HitboxComponent.class));
    assertNotNull(arrow.getComponent(ProjectileComponent.class));
    assertNull(arrow.getComponent(FollowComponent.class));
    // Arrow damage is the wielder's base attack scaled by the weapon multiplier: round(10 * 0.8).
    assertEquals(8, arrow.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldNotFireThroughAdjacentWall() {
    // Wall box starts at x = 0.9, between the wielder centre and the arrow spawn point.
    Entity wall =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));
    wall.setPosition(0.9f, 0f);
    wall.create();

    BowWeaponComponent bow = new BowWeaponComponent();
    Entity wielder =
        new Entity().addComponent(new WeaponStatsComponent(0.5f, 1f, 0f)).addComponent(bow);
    wielder.create();

    bow.attack(new Vector2(0.5f, 0.5f), new Vector2(1f, 0f));

    verify(entityService, never()).register(any());
  }
}
