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
  private static final int PRIORITY = 20;
  private static final float TRIGGER_RANGE = 3f;
  private static final float TELEGRAPH_DURATION = 0.5f;
  private static final float DASH_SPEED = 6f;
  private static final float DASH_DISTANCE = 4f;
  private static final float DASH_DURATION = 0.4f;
  private static final float COOLDOWN_DURATION = 2f;

  private enum Phase {
    TELEGRAPH,
    DASH,
    DONE
  }

  private final Entity target;
  private final float restoreSpeed;
  private final GameTime gameTime;

  private PhysicsMovementComponent movementComponent;
  private Phase phase;
  private long phaseStartTime;
  private long cooldownEndTime = 0;
  private Vector2 dashTargetPoint;

  /**
   * @param target Entity to lunge toward (usually the player).
   * @param restoreSpeed Normal movement speed to return to after the dash ends.
   */
  public LungeAttackTask(Entity target, float restoreSpeed) {
    this.target = target;
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
        if (now - phaseStartTime >= TELEGRAPH_DURATION * 1000) {
          beginDash(now);
        }
        break;
      case DASH:
        if (now - phaseStartTime >= DASH_DURATION * 1000 || reachedDashTarget()) {
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
      return phase == Phase.DONE ? -1 : PRIORITY;
    }

    long now = gameTime.getTime();
    if (now < cooldownEndTime) {
      return -1;
    }
    if (getDistanceToTarget() <= TRIGGER_RANGE) {
      return PRIORITY;
    }
    return -1;
  }

  private void beginDash(long now) {
    Vector2 direction = target.getPosition().cpy().sub(owner.getEntity().getPosition()).nor();
    dashTargetPoint = owner.getEntity().getPosition().cpy().add(direction.scl(DASH_DISTANCE));

    movementComponent.setMaxSpeed(new Vector2(DASH_SPEED, DASH_SPEED));
    movementComponent.setTarget(dashTargetPoint);
    movementComponent.setMoving(true);

    phase = Phase.DASH;
    phaseStartTime = now;
    owner.getEntity().getEvents().trigger("lungeDashStart");
  }

  private void endDash(long now) {
    movementComponent.setMoving(false);
    movementComponent.setMaxSpeed(new Vector2(restoreSpeed, restoreSpeed));

    cooldownEndTime = now + (long) (COOLDOWN_DURATION * 1000);
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
