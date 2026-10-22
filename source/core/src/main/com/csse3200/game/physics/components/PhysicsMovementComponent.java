package com.csse3200.game.physics.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.ai.movement.MovementController;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.utils.math.Vector2Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Movement controller for a physics-based entity. */
public class PhysicsMovementComponent extends Component implements MovementController {
  private static final Logger logger = LoggerFactory.getLogger(PhysicsMovementComponent.class);
  private Vector2 maxSpeed = Vector2Utils.ONE;

  private PhysicsComponent physicsComponent;
  private StatusEffectsControllerComponent statusEffects;
  private Vector2 targetPosition;
  private boolean limitToTarget;
  private boolean movementEnabled = true;
  private float staggerRemaining = 0f;

  @Override
  public void create() {
    physicsComponent = entity.getComponent(PhysicsComponent.class);
    // Looked up once: every steered entity asks this every frame, and an entity cannot gain a
    // controller after creation. An entity without one is simply never immobilised.
    statusEffects = entity.getComponent(StatusEffectsControllerComponent.class);
  }

  @Override
  public void update() {
    if (movementEnabled && targetPosition != null) {
      Body body = physicsComponent.getBody();
      if (staggerRemaining > 0f) {
        // Let a knockback impulse carry the body instead of steering cancelling it next frame.
        // Checked before immobilisation so a hit that also stuns (e.g. Medusa's hit recovery)
        // still pushes the enemy back before it is held still.
        staggerRemaining -= ServiceLocator.getTimeSource().getDeltaTime();
        return;
      }
      // Every steered entity stops here rather than each task learning to hold still, and the
      // target is left alone so the entity resumes its approach once the effect ends.
      if (statusEffects != null && statusEffects.isImmobilised()) {
        setToVelocity(body, Vector2.Zero);
        return;
      }
      updateDirection(body);
    }
  }

  /**
   * Enable/disable movement for the controller. Disabling will immediately set velocity to 0.
   *
   * @param movementEnabled true to enable movement, false otherwise
   */
  @Override
  public void setMoving(boolean movementEnabled) {
    this.movementEnabled = movementEnabled;
    if (!movementEnabled) {
      Body body = physicsComponent.getBody();
      setToVelocity(body, Vector2.Zero);
    }
  }

  @Override
  public boolean getMoving() {
    return movementEnabled;
  }

  /**
   * @return Target position in the world
   */
  @Override
  public Vector2 getTarget() {
    return targetPosition;
  }

  /**
   * Set a target to move towards. The entity will be steered towards it in a straight line, not
   * using pathfinding or avoiding other entities.
   *
   * @param target target position
   */
  @Override
  public void setTarget(Vector2 target) {
    setTarget(target, false);
  }

  /**
   * Sets a movement target, optionally limiting each update's travel to the remaining distance.
   * Navigation uses this to reach tight corners without overshooting on a long frame. Standard
   * movement and attacks retain their existing constant-speed behaviour through the one-argument
   * overload.
   *
   * @param target target position
   * @param limitToTarget whether to slow down when the target is less than one update away
   */
  public void setTarget(Vector2 target, boolean limitToTarget) {
    logger.trace("Setting target to {}", target);
    this.targetPosition = target;
    this.limitToTarget = limitToTarget;
  }

  /**
   * Sets the maximum movement speed for this entity. Defaults to (1,1) if never called.
   *
   * @param maxSpeed maximum speed vector
   */
  public void setMaxSpeed(Vector2 maxSpeed) {
    this.maxSpeed = maxSpeed;
  }

  /**
   * Suspends steering for a short time so an external impulse (e.g. weapon knockback) can move the
   * body. Without this, the next update applies an impulse back to the steering velocity and the
   * knockback is cancelled within one frame. A shorter stagger never shortens a longer one.
   *
   * @param duration seconds to suspend steering for
   * @require duration &gt;= 0
   * @throws IllegalArgumentException if duration is negative
   */
  public void stagger(float duration) {
    if (duration < 0f) {
      throw new IllegalArgumentException("stagger duration must be >= 0");
    }
    staggerRemaining = Math.max(staggerRemaining, duration);
  }

  /**
   * @return true while steering is suspended by {@link #stagger(float)}
   */
  public boolean isStaggered() {
    return staggerRemaining > 0f;
  }

  private void updateDirection(Body body) {
    Vector2 offset = targetPosition.cpy().sub(entity.getPosition());
    Vector2 desiredVelocity = offset.cpy().nor().scl(maxSpeed);
    if (limitToTarget) {
      // Physics may also consume a remainder accumulated in the preceding render frame.
      float travelTime =
          ServiceLocator.getTimeSource().getDeltaTime() + PhysicsEngine.PHYSICS_TIMESTEP;
      desiredVelocity.limit(offset.len() / travelTime);
    }
    setToVelocity(body, desiredVelocity);
  }

  private void setToVelocity(Body body, Vector2 desiredVelocity) {
    // impulse force = (desired velocity - current velocity) * mass
    Vector2 velocity = body.getLinearVelocity();
    Vector2 impulse = desiredVelocity.cpy().sub(velocity).scl(body.getMass());
    body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
  }
}
