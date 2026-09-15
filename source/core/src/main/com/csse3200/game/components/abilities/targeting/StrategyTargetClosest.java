package com.csse3200.game.components.abilities.targeting;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/**
 * Selects the single enemy closest to the caster (by centre-to-centre distance). Returns an empty
 * array if no enemies are registered.
 */
public class StrategyTargetClosest implements EnemyTargetingStrategy {
  @Override
  public Array<Entity> selectTargets(Entity caster) {
    Vector2 casterCenter = caster.getCenterPosition();
    Entity closest = null;
    float closestDistSq = Float.MAX_VALUE;

    for (Entity candidate : ServiceLocator.getEntityService().getEntities()) {
      if (candidate.equals(caster) || !EnemyUtils.isEnemy(candidate)) {
        continue;
      }
      float distSq = candidate.getCenterPosition().dst2(casterCenter);
      if (distSq < closestDistSq) {
        closestDistSq = distSq;
        closest = candidate;
      }
    }

    Array<Entity> targets = new Array<>();
    if (closest != null) {
      targets.add(closest);
    }
    return targets;
  }
}
