package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ProjectileComponentTest {
  private EntityService entityService;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);
  }

  private static Entity createProjectile(Vector2 direction, float speed) {
    Entity projectile =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.KinematicBody))
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.WEAPON))
            .addComponent(new ProjectileComponent(direction, speed));
    projectile.create();
    return projectile;
  }

  @Test
  void shouldTravelInGivenDirection() {
    Entity projectile = createProjectile(new Vector2(0f, 1f), 5f);

    Vector2 velocity =
        projectile.getComponent(PhysicsComponent.class).getBody().getLinearVelocity();
    assertEquals(0f, velocity.x, 1e-4f);
    assertEquals(5f, velocity.y, 1e-4f);
  }

  @Test
  void shouldDespawnOnEnemyHit() {
    Entity projectile = createProjectile(new Vector2(1f, 0f), 5f);
    Entity enemy =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC));
    enemy.create();

    projectile
        .getEvents()
        .trigger(
            "collisionStart",
            projectile.getComponent(HitboxComponent.class).getFixture(),
            enemy.getComponent(HitboxComponent.class).getFixture());

    verify(entityService).scheduleDisposal(projectile);
  }
}
