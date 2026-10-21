package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.raycast.RaycastHit;
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
    if (reachedMaxDistance || !Float.isFinite(deltaTime) || deltaTime <= 0f) {
      return;
    }

    float distanceLeft = maxDistance - distanceTravelled;
    float moveDistance = Math.min(speed * deltaTime, distanceLeft);
    Vector2 displacement = direction.cpy().scl(moveDistance);

    if (displacement.isZero()) {
      return;
    }

    Vector2 from = entity.getCenterPosition();
    Vector2 to = from.cpy().add(displacement);

    boolean blocked =
        ServiceLocator.getPhysicsService()
            .getPhysics()
            .raycast(from, to, PhysicsLayer.OBSTACLE, new RaycastHit());

    if (blocked) {
      reachedMaxDistance = true;
      ServiceLocator.getEntityService().scheduleDisposal(entity);
      return;
    }

    entity.setPosition(entity.getPosition().add(displacement));
    distanceTravelled += moveDistance;

    if (moveDistance >= distanceLeft) {
      reachedMaxDistance = true;
      entity.getEvents().trigger("projectileRangeReached");
    }
  }
}
