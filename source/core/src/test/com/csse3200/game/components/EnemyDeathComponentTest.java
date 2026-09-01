package com.csse3200.game.components;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
class EnemyDeathComponentTest {
  private EntityService entityService;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);
  }

  /** An enemy with the physics components that make disposal order matter. */
  private static Entity createEnemy() {
    return new Entity()
        .addComponent(new PhysicsComponent())
        .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
        .addComponent(new CombatStatsComponent(10, 0))
        .addComponent(new EnemyDeathComponent());
  }

  @Test
  void shouldNotDisposeEntityWhileDeathEventIsHandled() {
    Entity enemy = createEnemy();
    entityService.register(enemy);

    enemy.getEvents().trigger("entityDied");

    verify(entityService, never()).unregister(enemy);
  }

  @Test
  void shouldDisposeEntityOnceAfterDeathEvent() {
    Entity enemy = createEnemy();
    entityService.register(enemy);

    enemy.getEvents().trigger("entityDied");
    entityService.update();

    verify(entityService, times(1)).unregister(enemy);
  }

  @Test
  void shouldNotDisposeEntityWhenLethalDamageComesFromCollision() {
    Entity enemy = createEnemy();
    entityService.register(enemy);

    enemy.getComponent(CombatStatsComponent.class).takeDamage(10);

    verify(entityService, never()).unregister(enemy);
  }
}
