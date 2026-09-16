package com.csse3200.game.components.spells.targeting;

import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.enemyAt;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.nonEnemyAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit tests for {@link StrategyClosest}.
 *
 * <p>See {@link AllEnemiesTargetingStrategyTest} for the scaffolding assumptions shared by this
 * test class.
 */
@ExtendWith(GameExtension.class)
class StrategyClosestTest {
  private EntityService entityService;
  private StrategyClosest strategy;
  private Entity caster;

  @BeforeEach
  void setUp() {
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    strategy = new StrategyClosest();
    caster = mock(Entity.class);
    when(caster.getCenterPosition()).thenReturn(new Vector2(0f, 0f));
  }

  private void givenWorldEntities(Entity... entities) {
    Array<Entity> array = new Array<>();
    for (Entity entity : entities) {
      array.add(entity);
    }
    when(entityService.getEntities()).thenReturn(array);
  }

  @Test
  void returnsOnlyTheClosestEnemy() {
    Entity near = enemyAt(1f, 0f);
    Entity far = enemyAt(10f, 0f);
    givenWorldEntities(far, near);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(1, targets.size);
    assertSame(near, targets.first());
  }

  @Test
  void onATieKeepsTheFirstEnemyEncountered() {
    // Both equidistant (25 units^2) from the caster at (0,0). The implementation's comparison is
    // a strict "<", so it keeps whichever is encountered first in iteration order rather than the
    // last one seen.
    Entity first = enemyAt(5f, 0f);
    Entity second = enemyAt(-5f, 0f);
    givenWorldEntities(first, second);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(1, targets.size);
    assertSame(first, targets.first());
  }

  @Test
  void excludesTheCasterEvenIfItIsOnTheEnemyLayer() {
    Entity npcCaster = enemyAt(0f, 0f);
    givenWorldEntities(npcCaster);

    Array<Entity> targets = strategy.selectTargets(npcCaster);

    assertEquals(0, targets.size);
  }

  @Test
  void ignoresNonEnemyEntitiesEvenWhenCloser() {
    Entity closerNonEnemy = nonEnemyAt(0.5f, 0f);
    Entity fartherEnemy = enemyAt(3f, 0f);
    givenWorldEntities(closerNonEnemy, fartherEnemy);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(1, targets.size);
    assertSame(fartherEnemy, targets.first());
  }

  @Test
  void returnsEmptyArrayWhenNoEnemiesArePresent() {
    givenWorldEntities(caster);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(0, targets.size);
  }
}
