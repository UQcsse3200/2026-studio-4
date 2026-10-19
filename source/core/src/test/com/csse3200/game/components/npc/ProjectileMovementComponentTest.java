package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.raycast.RaycastHit;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ProjectileMovementComponentTest {

  private PhysicsEngine physics;
  private EntityService entityService;

  @BeforeEach
  void setUp() {
    physics = mock(PhysicsEngine.class);
    entityService = mock(EntityService.class);

    ServiceLocator.registerPhysicsService(new PhysicsService(physics));
    ServiceLocator.registerEntityService(entityService);
  }

  @Test
  void shouldMoveInGivenDirection() {
    ProjectileMovementComponent movement =
        new ProjectileMovementComponent(new Vector2(1f, 0f), 5f, 7f);
    Entity projectile = new Entity().addComponent(movement);
    projectile.setPosition(2f, 3f);

    movement.update(0.5f);

    assertEquals(4.5f, projectile.getPosition().x);
    assertEquals(3f, projectile.getPosition().y);
  }

  @Test
  void shouldStopAndPlayEffectAtMaximumRange() {
    ProjectileMovementComponent movement =
        new ProjectileMovementComponent(new Vector2(1f, 0f), 5f, 7f);
    Entity projectile = new Entity().addComponent(movement);
    EventListener0 callback = mock(EventListener0.class);
    projectile.getEvents().addListener("projectileRangeReached", callback);

    movement.update(2f);
    movement.update(1f);

    assertEquals(7f, projectile.getPosition().x);
    verify(callback, times(1)).handle();
  }

  @Test
  void shouldStopAtObstacleAndQueueRemovalOnlyOnce() {
    when(physics.raycast(
            any(Vector2.class),
            any(Vector2.class),
            eq(PhysicsLayer.OBSTACLE),
            any(RaycastHit.class)))
        .thenReturn(true);

    ProjectileMovementComponent movement =
        new ProjectileMovementComponent(new Vector2(1f, 0f), 5f, 7f);
    Entity projectile = new Entity().addComponent(movement);
    projectile.setPosition(2f, 3f);

    movement.update(0.5f);
    movement.update(0.5f);

    assertEquals(new Vector2(2f, 3f), projectile.getPosition());
    verify(entityService, times(1)).scheduleDisposal(projectile);
  }

  @Test
  void shouldCheckEntireMovementPathAgainstObstacles() {
    ProjectileMovementComponent movement =
        new ProjectileMovementComponent(new Vector2(1f, 0f), 5f, 7f);
    Entity projectile = new Entity().addComponent(movement);
    projectile.setScale(0.8f, 0.8f);
    projectile.setPosition(2f, 3f);

    Vector2 from = projectile.getCenterPosition().cpy();
    Vector2 to = from.cpy().add(5f, 0f);

    movement.update(1f);

    verify(physics).raycast(eq(from), eq(to), eq(PhysicsLayer.OBSTACLE), any(RaycastHit.class));
    assertEquals(7f, projectile.getPosition().x, 0.001f);
  }
}
