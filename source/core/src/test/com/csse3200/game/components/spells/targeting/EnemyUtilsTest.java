package com.csse3200.game.components.spells.targeting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** What every targeting strategy counts as an enemy. */
@ExtendWith(GameExtension.class)
class EnemyUtilsTest {

  @Test
  void treatsAnEntityOnTheNpcLayerAsAnEnemy() {
    assertTrue(EnemyUtils.isEnemy(TargetingTestHelper.enemyAt(0f, 0f)));
  }

  @Test
  void treatsAnEntityOnAnotherLayerAsNotAnEnemy() {
    assertFalse(EnemyUtils.isEnemy(TargetingTestHelper.nonEnemyAt(0f, 0f)));
  }

  @Test
  void treatsAnEntityWithoutAHitboxAsNotAnEnemy() {
    assertFalse(EnemyUtils.isEnemy(TargetingTestHelper.noHitboxAt(0f, 0f)));
  }

  @Test
  void treatsAHitboxWithNoFixtureYetAsNotAnEnemy() {
    // A hitbox only gets its fixture once the physics world builds it, so an entity created this
    // frame can legitimately be asked about before then.
    HitboxComponent hitbox = mock(HitboxComponent.class);
    when(hitbox.getFixture()).thenReturn(null);
    Entity entity = mock(Entity.class);
    when(entity.getComponent(HitboxComponent.class)).thenReturn(hitbox);

    assertFalse(EnemyUtils.isEnemy(entity));
  }

  @Test
  void cannotBeInstantiated() {
    java.lang.reflect.Constructor<EnemyUtils> constructor;
    try {
      constructor = EnemyUtils.class.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
    constructor.setAccessible(true);

    java.lang.reflect.InvocationTargetException thrown =
        assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);

    assertInstanceOf(IllegalStateException.class, thrown.getCause());
  }

  @Test
  void recognisesAnEnemyOnSeveralLayersAtOnce() {
    Entity entity = mock(Entity.class);
    HitboxComponent hitbox = mock(HitboxComponent.class);
    com.badlogic.gdx.physics.box2d.Fixture fixture =
        mock(com.badlogic.gdx.physics.box2d.Fixture.class);
    com.badlogic.gdx.physics.box2d.Filter filter = new com.badlogic.gdx.physics.box2d.Filter();
    filter.categoryBits = (short) (PhysicsLayer.NPC | PhysicsLayer.OBSTACLE);
    when(fixture.getFilterData()).thenReturn(filter);
    when(hitbox.getFixture()).thenReturn(fixture);
    when(entity.getComponent(HitboxComponent.class)).thenReturn(hitbox);

    assertTrue(EnemyUtils.isEnemy(entity));
  }
}
