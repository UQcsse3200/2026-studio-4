package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Performs a telegraphed lunge/dash attack toward a target. The entity freezes briefly (the
 * telegraph), then dashes quickly toward the target's last known position, then enters a cooldown
 * period before it can lunge again.
 */
public class LungeAttackTask extends DefaultTask implements PriorityTask {
  private enum Phase {
    TELEGRAPH,
    DASH,
    DONE
  }

  private final Entity target;
  private final int priority;
  private final float triggerRange;
  private final float telegraphDuration;
  private final float dashSpeed;
  private final float dashDistance;
  private final float dashDuration;
  private final float cooldownDuration;
  private final float restoreSpeed;

  private final GameTime gameTime;
  private PhysicsMovementComponent movementComponent;

  private Phase phase;
  private long phaseStartTime;
  private long cooldownEndTime = 0;
  private Vector2 dashTargetPoint;

  /**
   * @param target Entity to lunge toward (usually the player).
   * @param priority Priority while lunging (should be higher than the chase task's priority).
   * @param triggerRange Distance at which the lunge attack starts.
   * @param telegraphDuration Seconds the entity freezes before dashing.
   * @param dashSpeed Speed while dashing.
   * @param dashDistance Distance covered by the dash.
   * @param dashDuration Maximum seconds the dash can last.
   * @param cooldownDuration Seconds before another lunge can start after this one finishes.
   * @param restoreSpeed Normal movement speed to return to after the dash ends.
   */
  public LungeAttackTask(
      Entity target,
      int priority,
      float triggerRange,
      float telegraphDuration,
      float dashSpeed,
      float dashDistance,
      float dashDuration,
      float cooldownDuration,
      float restoreSpeed) {
    this.target = target;
    this.priority = priority;
    this.triggerRange = triggerRange;
    this.telegraphDuration = telegraphDuration;
    this.dashSpeed = dashSpeed;
    this.dashDistance = dashDistance;
    this.dashDuration = dashDuration;
    this.cooldownDuration = cooldownDuration;
    this.restoreSpeed = restoreSpeed;
    this.gameTime = ServiceLocator.getTimeSource();
  }

  @Override
  public void start() {
    super.start();
    movementComponent = owner.getEntity().getComponent(PhysicsMovementComponent.class);
    movementComponent.setMoving(false);
    phase = Phase.TELEGRAPH;
    phaseStartTime = gameTime.getTime();
    owner.getEntity().getEvents().trigger("lungeTelegraphStart");
  }

  @Override
  public void update() {
    long now = gameTime.getTime();
    switch (phase) {
      case TELEGRAPH:
        if (now - phaseStartTime >= telegraphDuration * 1000) {
          beginDash(now);
        }
        break;
      case DASH:
        if (now - phaseStartTime >= dashDuration * 1000 || reachedDashTarget()) {
          endDash(now);
        }
        break;
      case DONE:
        // Waiting for the AI component to switch to another task.
        break;
    }
  }

  @Override
  public void stop() {
    super.stop();
    if (movementComponent != null) {
      movementComponent.setMoving(false);
      movementComponent.setMaxSpeed(new Vector2(restoreSpeed, restoreSpeed));
    }
  }

  @Override
  public int getPriority() {
    if (status == Status.ACTIVE) {
      return phase == Phase.DONE ? -1 : priority;
    }

    long now = gameTime.getTime();
    if (now < cooldownEndTime) {
      return -1;
    }
    if (getDistanceToTarget() <= triggerRange) {
      return priority;
    }
    return -1;
  }

  private void beginDash(long now) {
    Vector2 direction = target.getPosition().cpy().sub(owner.getEntity().getPosition()).nor();
    dashTargetPoint = owner.getEntity().getPosition().cpy().add(direction.scl(dashDistance));

    movementComponent.setMaxSpeed(new Vector2(dashSpeed, dashSpeed));
    movementComponent.setTarget(dashTargetPoint);
    movementComponent.setMoving(true);

    phase = Phase.DASH;
    phaseStartTime = now;
    owner.getEntity().getEvents().trigger("lungeDashStart");
  }

  private void endDash(long now) {
    movementComponent.setMoving(false);
    movementComponent.setMaxSpeed(new Vector2(restoreSpeed, restoreSpeed));

    cooldownEndTime = now + (long) (cooldownDuration * 1000);
    phase = Phase.DONE;
    owner.getEntity().getEvents().trigger("lungeDashEnd");
  }

  private boolean reachedDashTarget() {
    return owner.getEntity().getPosition().dst(dashTargetPoint) <= 0.2f;
  }

  private float getDistanceToTarget() {
    return owner.getEntity().getPosition().dst(target.getPosition());
  }
}
