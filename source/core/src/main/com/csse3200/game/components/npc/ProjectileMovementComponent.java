package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;

/** Moves a floating demon projectile in one direction. */
public class ProjectileMovementComponent extends Component {
  private final Vector2 direction;
  private final float speed;
  private final float maxDistance;
  private float distanceTravelled;
  private boolean reachedMaxDistance;

  public ProjectileMovementComponent(Vector2 direction, float speed, float maxDistance) {
    this.direction = direction.cpy().nor();
    this.speed = speed;
    this.maxDistance = maxDistance;
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  public void update(float deltaTime) {
    if (reachedMaxDistance || deltaTime < 0f) {
      return;
    }

    float moveDistance = speed * deltaTime;
    float distanceLeft = maxDistance - distanceTravelled;

    if (moveDistance >= distanceLeft) {
      moveDistance = distanceLeft;
      reachedMaxDistance = true;
    }

    Vector2 newPosition = entity.getPosition();
    newPosition.mulAdd(direction, moveDistance);
    entity.setPosition(newPosition);
    distanceTravelled += moveDistance;

    if (reachedMaxDistance) {
      entity.getEvents().trigger("projectileRangeReached");
    }
  }
}
