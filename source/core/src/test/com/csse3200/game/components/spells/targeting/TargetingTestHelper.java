package com.csse3200.game.components.spells.targeting;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Builds the world a targeting strategy sees.
 *
 * <p>Entities are mocked rather than built for real, because a real hitbox fixture only exists once
 * a physics world has built it, which is a lot of setup for a test about which entities get picked.
 * {@code getComponent} is stubbed directly, sidestepping the fact that {@code Entity.addComponent}
 * keys components by exact runtime class and so would not match a mocked component by its declared
 * class.
 */
final class TargetingTestHelper {
  private TargetingTestHelper() {}

  /** An entity {@link EnemyUtils#isEnemy} recognises, centred at the given position. */
  static Entity enemyAt(float centerX, float centerY) {
    return entityAt(centerX, centerY, hitboxOnLayer(PhysicsLayer.NPC));
  }

  /** An entity on a non-NPC layer, e.g. the player, so {@code isEnemy} is false. */
  static Entity nonEnemyAt(float centerX, float centerY) {
    return entityAt(centerX, centerY, hitboxOnLayer(PhysicsLayer.PLAYER));
  }

  /** An entity with no hitbox at all, the other way {@code isEnemy} is false. */
  static Entity noHitboxAt(float centerX, float centerY) {
    return entityAt(centerX, centerY, null);
  }

  /** Registers exactly these entities as the world every strategy iterates over. */
  static void givenWorld(Entity... entities) {
    EntityService entityService = mock(EntityService.class);
    Array<Entity> world = new Array<>();
    for (Entity entity : entities) {
      world.add(entity);
    }
    when(entityService.getEntities()).thenReturn(world);
    ServiceLocator.registerEntityService(entityService);
  }

  private static Entity entityAt(float centerX, float centerY, HitboxComponent hitbox) {
    Entity entity = mock(Entity.class);
    // A fresh vector per call, matching the real Entity. Callers subtract in place, so handing out
    // one shared instance would leave every later read of this entity's position corrupted.
    when(entity.getCenterPosition()).thenAnswer(invocation -> new Vector2(centerX, centerY));
    when(entity.getComponent(HitboxComponent.class)).thenReturn(hitbox);
    return entity;
  }

  /**
   * Built outside any {@code when(...)}, since this stubs mocks of its own and Mockito treats
   * stubbing inside an unfinished stubbing as one unfinished stubbing.
   */
  private static HitboxComponent hitboxOnLayer(short layer) {
    Filter filter = new Filter();
    filter.categoryBits = layer;
    Fixture fixture = mock(Fixture.class);
    when(fixture.getFilterData()).thenReturn(filter);
    HitboxComponent hitbox = mock(HitboxComponent.class);
    when(hitbox.getFixture()).thenReturn(fixture);
    return hitbox;
  }
}
