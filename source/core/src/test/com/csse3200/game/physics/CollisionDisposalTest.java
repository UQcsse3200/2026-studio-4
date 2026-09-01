package com.csse3200.game.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.EnemyDeathComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Physics bodies and fixtures cannot be destroyed while the physics world is stepping, so an entity
 * killed by a collision must outlive the step that killed it.
 */
@ExtendWith(GameExtension.class)
class CollisionDisposalTest {
  private static final Vector2 SHARED_POSITION = new Vector2(5f, 5f);
  private static final int ENEMY_HEALTH = 10;

  private PhysicsEngine physicsEngine;
  private EntityService entityService;

  @BeforeEach
  void beforeEach() {
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(0.02f);
    ServiceLocator.registerTimeSource(gameTime);

    PhysicsService physicsService = new PhysicsService();
    ServiceLocator.registerPhysicsService(physicsService);
    physicsEngine = physicsService.getPhysics();

    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);
  }

  /** An enemy that dies from a single hit of {@link #ENEMY_HEALTH} damage. */
  private Entity registerEnemy() {
    Entity enemy =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new CombatStatsComponent(ENEMY_HEALTH, 0))
            .addComponent(new EnemyDeathComponent());
    enemy.setPosition(SHARED_POSITION);
    entityService.register(enemy);
    return enemy;
  }

  /** A weapon sensor overlapping the enemy, dealing exactly lethal damage. */
  private Entity registerWeaponHitbox() {
    HitboxSpec spec =
        new HitboxSpec()
            .position(SHARED_POSITION)
            .size(new Vector2(1f, 1f))
            .lifetime(0.5f)
            .layer(PhysicsLayer.WEAPON)
            .targetLayer(PhysicsLayer.NPC)
            .damage(ENEMY_HEALTH);
    Entity hitbox = HitboxFactory.createHitbox(spec);
    entityService.register(hitbox);
    return hitbox;
  }

  @Test
  void shouldNotDisposeEnemyDuringPhysicsStepThatKillsIt() {
    Entity enemy = registerEnemy();
    registerWeaponHitbox();

    physicsEngine.update();

    assertEquals(0, enemy.getComponent(CombatStatsComponent.class).getHealth());
    verify(entityService, never()).unregister(enemy);
  }

  @Test
  void shouldDisposeEnemyAfterPhysicsStepThatKillsIt() {
    Entity enemy = registerEnemy();
    registerWeaponHitbox();

    physicsEngine.update();
    entityService.update();

    verify(entityService, times(1)).unregister(enemy);
  }

  @Test
  void shouldKeepSteppingAfterDisposingEnemyKilledByCollision() {
    Entity enemy = registerEnemy();
    registerWeaponHitbox();

    physicsEngine.update();
    entityService.update();
    physicsEngine.update();

    verify(entityService, times(1)).unregister(enemy);
  }
}
