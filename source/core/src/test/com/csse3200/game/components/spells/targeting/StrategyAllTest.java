package com.csse3200.game.components.spells.targeting;

import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.enemyAt;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.givenWorld;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.noHitboxAt;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.nonEnemyAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** {@link StrategyAll}: every registered enemy, wherever it is. */
@ExtendWith(GameExtension.class)
class StrategyAllTest {
  private StrategyAll strategy;

  @BeforeEach
  void setUp() {
    strategy = new StrategyAll();
  }

  @Test
  void returnsEveryEnemyRegardlessOfDistance() {
    Entity caster = nonEnemyAt(0f, 0f);
    Entity near = enemyAt(1f, 1f);
    Entity veryFarAway = enemyAt(9000f, 9000f);
    givenWorld(caster, near, veryFarAway);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(2, targets.size);
    assertTrue(targets.contains(near, true));
    assertTrue(targets.contains(veryFarAway, true));
  }

  @Test
  void excludesTheCasterEvenWhenItIsItselfOnTheEnemyLayer() {
    Entity caster = enemyAt(0f, 0f);
    Entity enemy = enemyAt(1f, 1f);
    givenWorld(caster, enemy);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(1, targets.size);
    assertTrue(targets.contains(enemy, true));
  }

  @Test
  void excludesEntitiesThatAreNotEnemies() {
    Entity caster = nonEnemyAt(0f, 0f);
    Entity enemy = enemyAt(1f, 1f);
    givenWorld(caster, enemy, nonEnemyAt(2f, 2f), noHitboxAt(3f, 3f));

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(1, targets.size);
    assertTrue(targets.contains(enemy, true));
  }

  @Test
  void returnsEmptyRatherThanNullWhenTheWorldHoldsNoEnemies() {
    Entity caster = nonEnemyAt(0f, 0f);
    givenWorld(caster);

    Array<Entity> targets = strategy.selectTargets(caster);

    assertEquals(0, targets.size);
  }

  @Test
  void reportsNoRadiusBecauseItIsNotBoundedByOne() {
    // A spell reads this to size the area it draws; an unbounded strategy has no circle to show.
    assertEquals(0f, strategy.getRadius());
  }
}
