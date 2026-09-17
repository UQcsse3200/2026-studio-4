package com.csse3200.game.components.spells.targeting;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/**
 * Selects every enemy within a radius of the caster. Returns an empty array if none are in range.
 *
 * <p>The radius is fixed when the strategy is built so that whoever casts through it can also show
 * the area it covers, rather than the reach living in one place and the circle drawn in another.
 */
public class StrategyWithinRadius implements EnemyTargetingStrategy {
  private static final float DEFAULT_RADIUS = 10f;

  private final float radius;

  /** Uses the default radius of {@value #DEFAULT_RADIUS}. */
  public StrategyWithinRadius() {
    this(DEFAULT_RADIUS);
  }

  /**
   * @param radius distance from the centre of the caster, in world units
   * @throws IllegalArgumentException if radius is negative
   */
  public StrategyWithinRadius(float radius) {
    if (radius < 0f) {
      throw new IllegalArgumentException("radius must be >= 0");
    }
    this.radius = radius;
  }

  @Override
  public float getRadius() {
    return radius;
  }

  @Override
  public Array<Entity> selectTargets(Entity caster) {
    return selectTargets(caster, radius);
  }

  /**
   * Selects targets within a one-off radius, ignoring this strategy's own.
   *
   * @param caster the entity casting the spell
   * @param radius distance from the centre of the caster, in world units
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
      if (candidate.getCenterPosition().dst2(casterCenter) < radiusSq) {
        targets.add(candidate);
      }
    }

    return targets;
  }
}
