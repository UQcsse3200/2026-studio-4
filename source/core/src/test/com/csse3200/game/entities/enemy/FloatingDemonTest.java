package com.csse3200.game.entities.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.EnemyDeathComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FloatingDemonTest {
  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());

    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);

    ResourceService resourceService = new ResourceService();
    resourceService.loadTextureAtlases(new String[] {"images/floatingDemon.atlas"});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void shouldTakeDamageFromPlayerWeapon() {
    Entity player = new Entity();
    Entity demon = NPCFactory.createFloatingDemon(player, 1f, 10f);
    demon.create();

    HitboxComponent demonHitbox = demon.getComponent(HitboxComponent.class);
    assertNotNull(demonHitbox);
    assertTrue(demonHitbox.getFixture().isSensor());
    assertEquals(PhysicsLayer.NPC, demonHitbox.getLayer());
    assertNotNull(demon.getComponent(EnemyDeathComponent.class));
    assertEquals(77, demon.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(7, demon.getComponent(CombatStatsComponent.class).getBaseAttack());

    PhysicsMovementComponent movement = demon.getComponent(PhysicsMovementComponent.class);
    movement.setTarget(new Vector2(10f, 0f));
    movement.update();
    assertEquals(3f, demon.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().x);

    HitboxSpec weaponSpec =
        new HitboxSpec()
            .position(new Vector2(0f, 0f))
            .size(new Vector2(1f, 1f))
            .lifetime(1f)
            .layer(PhysicsLayer.WEAPON)
            .targetLayer(PhysicsLayer.NPC)
            .damage(5);
    Entity weapon = HitboxFactory.createHitbox(weaponSpec);
    weapon.create();

    Fixture weaponFixture = weapon.getComponent(HitboxComponent.class).getFixture();
    weapon.getEvents().trigger("collisionStart", weaponFixture, demonHitbox.getFixture());

    assertEquals(72, demon.getComponent(CombatStatsComponent.class).getHealth());
  }
}
