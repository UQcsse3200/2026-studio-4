package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FloatingDemonProjectileFactoryTest {
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
  void shouldDamagePlayerOnCollision() {
    Entity projectile =
        FloatingDemonProjectileFactory.createProjectile(
            new Vector2(0f, 0f), new Vector2(1f, 0f), 7);
    projectile.create();

    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 0))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    player.create();

    Fixture projectileFixture = projectile.getComponent(HitboxComponent.class).getFixture();
    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();
    projectile.getEvents().trigger("collisionStart", projectileFixture, playerFixture);

    assertEquals(13, player.getComponent(CombatStatsComponent.class).getHealth());
  }
}
