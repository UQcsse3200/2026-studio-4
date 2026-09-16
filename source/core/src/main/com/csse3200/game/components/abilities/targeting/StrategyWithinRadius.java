package com.csse3200.game.components.abilities.targeting;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/**
 * Selects all enemies within a custom radius or default radius of 10 from the caster returns an
 * empty array if no enemies are registered
 */
public class StrategyWithinRadius implements EnemyTargetingStrategy {

  public Array<Entity> selectTargets(Entity caster) {
    Vector2 casterCenter = caster.getCenterPosition();
    float radius = 10f;
    float radiusSq = radius * radius;
    Array<Entity> targets = new Array<>();

    for (Entity candidate : ServiceLocator.getEntityService().getEntities()) {
      if (candidate.equals(caster) || !EnemyUtils.isEnemy(candidate)) {
        continue;
      }
      float distSq = candidate.getCenterPosition().dst2(casterCenter);
      if (distSq < radiusSq) {
        targets.add(candidate);
      }
    }

    return targets;
  }

  /**
   * Selects targets within given radius
   *
   * @param caster the player entity
   * @param radius distance from the center of the player entity
   * @return list of enemy entities satisfying the targeting conditions
   */
  public Array<Entity> selectTargets(Entity caster, float radius) {
    Vector2 casterCenter = caster.getCenterPosition();
    float radiusSq = radius * radius;
    Array<Entity> targets = new Array<>();

    for (Entity candidate : ServiceLocator.getEntityService().getEntities()) {
      if (candidate.equals(caster) || !EnemyUtils.isEnemy(candidate)) {
        continue;
      }
      float distSq = candidate.getCenterPosition().dst2(casterCenter);
      if (distSq < radiusSq) {
        targets.add(candidate);
      }
    }

    return targets;
  }
}
