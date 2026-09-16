package com.csse3200.game.components.spells.targeting;

import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.enemyAt;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.givenWorld;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.noHitboxAt;
import static com.csse3200.game.components.spells.targeting.TargetingTestHelper.nonEnemyAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** {@link StrategyWithinRadius}: every enemy inside the caster's circle. */
@ExtendWith(GameExtension.class)
class StrategyWithinRadiusTest {

  @Test
  void selectsOnlyTheEnemiesInsideTheRadius() {
    Entity caster = nonEnemyAt(0f, 0f);
    Entity inside = enemyAt(2f, 0f);
    Entity justOutside = enemyAt(3.5f, 0f);
    givenWorld(caster, inside, justOutside);

    Array<Entity> targets = new StrategyWithinRadius(3f).selectTargets(caster);

    assertEquals(1, targets.size);
    assertTrue(targets.contains(inside, true));
    assertFalse(targets.contains(justOutside, true));
  }

  @Test
  void measuresFromTheCasterRatherThanTheOrigin() {
    Entity caster = nonEnemyAt(20f, 20f);
    Entity besideTheCaster = enemyAt(21f, 20f);
    Entity atTheOrigin = enemyAt(0f, 0f);
    givenWorld(caster, besideTheCaster, atTheOrigin);

    Array<Entity> targets = new StrategyWithinRadius(3f).selectTargets(caster);

    assertEquals(1, targets.size);
    assertTrue(targets.contains(besideTheCaster, true));
  }

  @Test
  void excludesAnEnemyExactlyOnTheEdge() {
    // The boundary is exclusive. Pinned here because the drawn circle is sized from this radius,
    // and an enemy visibly on the rim being hit or missed is exactly what a player would query.
    Entity caster = nonEnemyAt(0f, 0f);
    Entity onTheRim = enemyAt(3f, 0f);
    givenWorld(caster, onTheRim);

    assertEquals(0, new StrategyWithinRadius(3f).selectTargets(caster).size);
  }

  @Test
  void measuresDiagonallyRatherThanPerAxis() {
    Entity caster = nonEnemyAt(0f, 0f);
    // Inside the bounding box of radius 5, but 7.07 away, so outside the circle.
    Entity diagonal = enemyAt(5f, 5f);
    givenWorld(caster, diagonal);

    assertEquals(0, new StrategyWithinRadius(5f).selectTargets(caster).size);
  }

  @Test
  void excludesTheCasterAndEverythingThatIsNotAnEnemy() {
    Entity caster = enemyAt(0f, 0f);
    Entity enemy = enemyAt(1f, 0f);
    givenWorld(caster, enemy, nonEnemyAt(1f, 0f), noHitboxAt(1f, 0f));

    Array<Entity> targets = new StrategyWithinRadius(5f).selectTargets(caster);

    assertEquals(1, targets.size);
    assertTrue(targets.contains(enemy, true));
  }

  @Test
  void defaultsToATenUnitRadius() {
    Entity caster = nonEnemyAt(0f, 0f);
    Entity inside = enemyAt(9f, 0f);
    Entity outside = enemyAt(11f, 0f);
    givenWorld(caster, inside, outside);

    StrategyWithinRadius strategy = new StrategyWithinRadius();

    assertEquals(10f, strategy.getRadius());
    Array<Entity> targets = strategy.selectTargets(caster);
    assertEquals(1, targets.size);
    assertTrue(targets.contains(inside, true));
  }

  @Test
  void reportsTheRadiusItWasBuiltWithSoASpellCanDrawTheSameCircleItHits() {
    assertEquals(3f, new StrategyWithinRadius(3f).getRadius());
  }

  @Test
  void rejectsANegativeRadius() {
    assertThrows(IllegalArgumentException.class, () -> new StrategyWithinRadius(-1f));
  }

  @Test
  void aZeroRadiusHitsNothing() {
    Entity caster = nonEnemyAt(0f, 0f);
    givenWorld(caster, enemyAt(0f, 0f));

    assertEquals(0, new StrategyWithinRadius(0f).selectTargets(caster).size);
  }

  @Test
  void theOneOffOverloadUsesTheRadiusPassedInsteadOfItsOwn() {
    Entity caster = nonEnemyAt(0f, 0f);
    Entity far = enemyAt(50f, 0f);
    givenWorld(caster, far);
    StrategyWithinRadius strategy = new StrategyWithinRadius(1f);

    assertEquals(0, strategy.selectTargets(caster).size);
    assertEquals(1, strategy.selectTargets(caster, 100f).size);
    assertEquals(1f, strategy.getRadius(), "a one-off reach must not change the strategy");
  }
}
