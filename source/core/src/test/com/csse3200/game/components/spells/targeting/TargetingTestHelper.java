package com.csse3200.game.components.spells.targeting;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;

/**
 * Shared helpers for building mock {@link Entity} instances that {@link EnemyUtils#isEnemy} does or
 * doesn't recognise, for use across the targeting strategy tests.
 *
 * <p>Entities are mocked directly (stubbing {@code getComponent(HitboxComponent.class)}) rather
 * than built as real {@link Entity} instances with a real {@link HitboxComponent} attached. That
 * avoids two problems: a real hitbox fixture only exists once a physics world has built it (heavy
 * to set up for a targeting-logic test), and {@code Entity.addComponent} keys components by the
 * exact runtime class of whatever is added, so attaching a Mockito-mocked component risks not
 * matching a later {@code getComponent(HitboxComponent.class)} lookup by its declared class.
 * Stubbing {@code getComponent} directly on a mocked {@code Entity} sidesteps that registry
 * entirely.
 */
final class TargetingTestHelper {
  private TargetingTestHelper() {}

  /** A mock entity on the NPC physics layer, with its centre at the given position. */
  static Entity enemyAt(float centerX, float centerY) {
    Entity entity = mock(Entity.class);
    when(entity.getCenterPosition()).thenReturn(new Vector2(centerX, centerY));
    when(entity.getComponent(HitboxComponent.class)).thenReturn(hitboxOnLayer(PhysicsLayer.NPC));
    return entity;
  }

  /** A mock entity on a non-NPC physics layer (e.g. the player), so {@code isEnemy} is false. */
  static Entity nonEnemyAt(float centerX, float centerY) {
    Entity entity = mock(Entity.class);
    when(entity.getCenterPosition()).thenReturn(new Vector2(centerX, centerY));
    when(entity.getComponent(HitboxComponent.class)).thenReturn(hitboxOnLayer(PhysicsLayer.PLAYER));
    return entity;
  }

  /** A mock entity with no hitbox at all, another way {@code isEnemy} is false. */
  static Entity noHitboxAt(float centerX, float centerY) {
    Entity entity = mock(Entity.class);
    when(entity.getCenterPosition()).thenReturn(new Vector2(centerX, centerY));
    when(entity.getComponent(HitboxComponent.class)).thenReturn(null);
    return entity;
  }

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
