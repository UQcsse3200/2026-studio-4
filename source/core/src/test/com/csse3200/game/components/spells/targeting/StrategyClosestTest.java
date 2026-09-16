package com.csse3200.game.components.spells.targeting;

import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.enemyAt;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.givenWorld;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.noHitboxAt;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.nonEnemyAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** {@link StrategyClosest}: the single nearest enemy. */
@ExtendWith(GameExtension.class)
class StrategyClosestTest {
  private StrategyClosest strategy;

  @BeforeEach
  void setUp() {
    strategy = new StrategyClosest();
  }

  @Test
  void returnsOnlyTheNearestEnemy() {
    Entity caster = nonEnemyAt(0f, 0f);
    Entity near = enemyAt(1f, 0f);
    Entity far = enemyAt(8f, 0f);
    givenWorld(caster, far, near);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(1, targets.size);
    assertSame(near, targets.get(0));
  }

  @Test
  void measuresDistanceFromTheCasterRatherThanTheOrigin() {
    Entity caster = nonEnemyAt(10f, 10f);
    Entity nearTheOrigin = enemyAt(1f, 1f);
    Entity nearTheCaster = enemyAt(11f, 10f);
    givenWorld(caster, nearTheOrigin, nearTheCaster);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertSame(nearTheCaster, targets.get(0));
  }

  @Test
  void keepsTheFirstEnemyEncounteredWhenTwoAreEquallyClose() {
    Entity caster = nonEnemyAt(0f, 0f);
    Entity first = enemyAt(3f, 0f);
    Entity second = enemyAt(0f, 3f);
    givenWorld(caster, first, second);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(1, targets.size);
    assertSame(first, targets.get(0));
  }

  @Test
  void ignoresNonEnemiesEvenWhenTheyAreCloser() {
    Entity caster = nonEnemyAt(0f, 0f);
    Entity enemy = enemyAt(5f, 0f);
    givenWorld(caster, nonEnemyAt(1f, 0f), noHitboxAt(2f, 0f), enemy);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(1, targets.size);
    assertSame(enemy, targets.get(0));
  }

  @Test
  void excludesTheCasterEvenWhenItIsTheNearestThingOnTheEnemyLayer() {
    Entity caster = enemyAt(0f, 0f);
    Entity enemy = enemyAt(5f, 0f);
    givenWorld(caster, enemy);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(1, targets.size);
    assertSame(enemy, targets.get(0));
  }

  @Test
  void returnsEmptyWhenThereIsNothingToTarget() {
    Entity caster = nonEnemyAt(0f, 0f);
    givenWorld(caster, nonEnemyAt(1f, 1f));

    assertEquals(0, strategy.selectTargets(caster).size);
  }

  @Test
  void reportsNoRadiusBecauseItIsNotBoundedByOne() {
    assertEquals(0f, strategy.getRadius());
  }
}
