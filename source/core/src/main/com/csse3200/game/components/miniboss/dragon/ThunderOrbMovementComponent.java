package com.csse3200.game.components.miniboss.dragon;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/** Moves a homing orb with limited turning speed and a finite lifetime. */
public class ThunderOrbMovementComponent extends Component {
  public static final String EXPIRED = "thunderOrbExpired";

  private final Entity target;
  private final float speed;
  private final float turnSpeed;
  private final float lifetime;

  private final Vector2 direction = new Vector2(1f, 0f);
  private float age;
  private boolean stopped;

  /**
   * Creates homing movement.
   *
   * @param target entity to follow
   * @param speed movement speed in world units per second
   * @param turnSpeed maximum turn speed in degrees per second
   * @param lifetime lifetime in seconds
   */
  public ThunderOrbMovementComponent(Entity target, float speed, float turnSpeed, float lifetime) {
    if (target == null
        || !Float.isFinite(speed)
        || speed <= 0f
        || !Float.isFinite(turnSpeed)
        || turnSpeed <= 0f
        || !Float.isFinite(lifetime)
        || lifetime <= 0f) {
      throw new IllegalArgumentException(
          "Target must exist and movement settings must be finite and positive");
    }
    this.target = target;
    this.speed = speed;
    this.turnSpeed = turnSpeed;
    this.lifetime = lifetime;
  }

  @Override
  public void create() {
    Vector2 offset = target.getCenterPosition().sub(entity.getCenterPosition());
    if (!offset.isZero()) {
      direction.set(offset).nor();
    }
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  /**
   * Advances movement and lifetime by the supplied elapsed time.
   *
   * @param delta elapsed time in seconds
   */
  public void update(float delta) {
    if (stopped || !Float.isFinite(delta) || delta <= 0f) {
      return;
    }

    float remaining = lifetime - age;
    float stepTime = Math.min(delta, remaining);

    turnTowardsTarget(stepTime);
    entity.setPosition(entity.getPosition().mulAdd(direction, speed * stepTime));

    age += stepTime;
    if (delta >= remaining) {
      stop();
      entity.getEvents().trigger(EXPIRED);
    }
  }

  private void turnTowardsTarget(float delta) {
    Vector2 offset = target.getCenterPosition().sub(entity.getCenterPosition());
    if (offset.isZero()) {
      return;
    }

    float difference = offset.angleDeg() - direction.angleDeg();
    difference = (difference + 540f) % 360f - 180f;

    float maximumTurn = turnSpeed * delta;
    float turn = MathUtils.clamp(difference, -maximumTurn, maximumTurn);
    direction.rotateDeg(turn).nor();
  }

  /** Permanently stops movement and lifetime updates, for example after a hit. */
  public void stop() {
    stopped = true;
  }

  /** Returns whether movement has stopped through impact or expiry. */
  public boolean isStopped() {
    return stopped;
  }
}
