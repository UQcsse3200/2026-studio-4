package com.csse3200.game.components.miniboss.cerberus;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/** Follows the target's current position until its travel range is exhausted. */
public class HomingProjectileMovementComponent extends Component {
  private final Entity target;
  private final float speed;
  private final float maxDistance;

  private float travelled;
  private boolean finished;

  public HomingProjectileMovementComponent(Entity target, float speed, float maxDistance) {
    if (target == null
        || !Float.isFinite(speed)
        || !Float.isFinite(maxDistance)
        || speed <= 0f
        || maxDistance <= 0f) {
      throw new IllegalArgumentException("Target, speed and range must be valid");
    }

    this.target = target;
    this.speed = speed;
    this.maxDistance = maxDistance;
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  public void update(float delta) {
    if (finished || !Float.isFinite(delta) || delta <= 0f) {
      return;
    }

    Vector2 direction = target.getCenterPosition().sub(entity.getCenterPosition());
    float distanceToTarget = direction.len();
    if (distanceToTarget == 0f) {
      return;
    }
    float remaining = maxDistance - travelled;
    float step = Math.min(Math.min(speed * delta, remaining), distanceToTarget);

    entity.setPosition(entity.getPosition().mulAdd(direction.nor(), step));
    travelled += step;

    if (step >= remaining) {
      finished = true;
      entity.getEvents().trigger("projectileRangeReached");
    }
  }
}
