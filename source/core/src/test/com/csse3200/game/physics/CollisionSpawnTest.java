package com.csse3200.game.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.SplitComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Physics bodies and fixtures cannot be created while the physics world is stepping, so an enemy
 * split by a collision must not build its children until the step that split it has finished.
 */
@ExtendWith(GameExtension.class)
class CollisionSpawnTest {
  private static final Vector2 SHARED_POSITION = new Vector2(5f, 5f);
  private static final int ENEMY_HEALTH = 20;
  private static final int WEAPON_DAMAGE = 10;

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

    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);

    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);

    ResourceService resourceService = new ResourceService();
    resourceService.loadTextureAtlases(new String[] {"images/chaseEnemy.atlas"});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  /** An enemy that survives one hit of {@link #WEAPON_DAMAGE} damage and splits in response. */
  private Entity registerSplitEnemy() {
    Entity enemy =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new CombatStatsComponent(ENEMY_HEALTH, 0))
            .addComponent(new SplitComponent(new Entity()));
    enemy.setPosition(SHARED_POSITION);
    entityService.register(enemy);
    return enemy;
  }

  /** A weapon sensor overlapping the enemy, dealing non-lethal damage. */
  private Entity registerWeaponHitbox() {
    HitboxSpec spec =
        new HitboxSpec()
            .position(SHARED_POSITION)
            .size(new Vector2(1f, 1f))
            .lifetime(0.5f)
            .layer(PhysicsLayer.WEAPON)
            .targetLayer(PhysicsLayer.NPC)
            .damage(WEAPON_DAMAGE);
    Entity hitbox = HitboxFactory.createHitbox(spec);
    entityService.register(hitbox);
    return hitbox;
  }

  @SuppressWarnings("unchecked")
  private static EventListener1<Entity> addChildListener(Entity enemy) {
    EventListener1<Entity> childListener = mock(EventListener1.class);
    enemy.getEvents().addListener("spawnChildren", childListener);
    return childListener;
  }

  @Test
  void shouldNotSpawnChildrenDuringPhysicsStepThatSplitsEnemy() {
    Entity enemy = registerSplitEnemy();
    EventListener1<Entity> childListener = addChildListener(enemy);
    registerWeaponHitbox();

    physicsEngine.update();

    assertEquals(
        ENEMY_HEALTH - WEAPON_DAMAGE, enemy.getComponent(CombatStatsComponent.class).getHealth());
    verify(childListener, times(0)).handle(any());
  }

  @Test
  void shouldSpawnChildrenAfterPhysicsStepThatSplitsEnemy() {
    Entity enemy = registerSplitEnemy();
    EventListener1<Entity> childListener = addChildListener(enemy);
    registerWeaponHitbox();

    physicsEngine.update();
    entityService.update();

    verify(childListener, times(2)).handle(any());
    verify(entityService, times(1)).unregister(enemy);
  }

  @Test
  void shouldKeepSteppingAfterSplittingEnemy() {
    Entity enemy = registerSplitEnemy();
    enemy.getEvents().addListener("spawnChildren", (Entity child) -> entityService.register(child));
    registerWeaponHitbox();

    physicsEngine.update();
    entityService.update();
    physicsEngine.update();
    entityService.update();

    verify(entityService, times(1)).unregister(enemy);
  }
}
