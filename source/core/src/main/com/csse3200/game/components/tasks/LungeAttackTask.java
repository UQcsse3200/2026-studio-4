package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.movement.ObstacleNavigation;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.StatusEffectsControllerComponent;
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
  private static final float TRIGGER_RANGE = 3f;
  private static final float TELEGRAPH_DURATION = 0.5f;
  private static final float DASH_SPEED = 6f;
  private static final float DASH_DISTANCE = 4f;
  private static final float DASH_STOP_BUFFER = 0.6f;
  private static final float DASH_DURATION = 0.4f;
  private static final float DEFAULT_COOLDOWN_DURATION = 2f;

  private enum Phase {
    TELEGRAPH,
    DASH,
    DONE
  }

  private final Entity target;
  private final float restoreSpeed;
  private final long cooldownDurationMillis;
  private final GameTime gameTime;
  private PhysicsMovementComponent movementComponent;
  private Phase phase;
  private long phaseStartTime;
  private long cooldownEndTime = 0;
  private Vector2 dashTargetPoint;
  private int priority = 0;
  private Entity enemy;

  /**
   * @param target Entity to lunge toward (usually the player).
   * @param priority task priority when a lunge is available
   * @param restoreSpeed Normal movement speed to return to after the dash ends.
   * @param enemy entity performing the lunge
   */
  public LungeAttackTask(Entity target, int priority, float restoreSpeed, Entity enemy) {
    this(target, priority, restoreSpeed, enemy, DEFAULT_COOLDOWN_DURATION);
  }

  /**
   * Creates a lunge with a configurable recovery between attacks.
   *
   * @param target entity to lunge toward
   * @param priority task priority when a lunge is available
   * @param restoreSpeed normal movement speed after a dash ends
   * @param enemy entity performing the lunge
   * @param cooldownDuration seconds before another lunge after finishing or interrupting one
   * @throws IllegalArgumentException if the cooldown is not finite and positive
   */
  public LungeAttackTask(
      Entity target, int priority, float restoreSpeed, Entity enemy, float cooldownDuration) {
    if (!Float.isFinite(cooldownDuration) || cooldownDuration <= 0f) {
      throw new IllegalArgumentException("Lunge cooldown must be finite and positive");
    }
    this.target = target;
    this.restoreSpeed = restoreSpeed;
    this.cooldownDurationMillis = (long) (cooldownDuration * 1000);
    this.priority = priority;
    this.enemy = enemy;
    this.gameTime = ServiceLocator.getTimeSource();
    movementComponent = enemy.getComponent(PhysicsMovementComponent.class);
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
    if (StatusEffectsControllerComponent.isConcealed(target)) {
      loseTarget(now);
      return;
    }
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

  /**
   * Cancels a pending or active lunge and starts its cooldown again from now.
   *
   * <p>A dash announces its end only if it had started. When another task owns movement, this only
   * delays the next lunge and leaves that task's movement settings alone.
   */
  public void interrupt() {
    boolean wasDashing = status == Status.ACTIVE && phase == Phase.DASH;
    if (status == Status.ACTIVE && phase != Phase.DONE) {
      movementComponent.setMoving(false);
      movementComponent.setMaxSpeed(new Vector2(restoreSpeed, restoreSpeed));
    }
    cooldownEndTime = gameTime.getTime() + cooldownDurationMillis;
    phase = Phase.DONE;
    if (wasDashing) {
      enemy.getEvents().trigger("lungeDashEnd");
    }
  }

  /**
   * A pure query: the scheduler calls this repeatedly, so it never changes state. While the lunge
   * is in progress it keeps its priority even if the target has just vanished, so that {@link
   * #update()} still runs and can wind the lunge down itself before this yields.
   */
  @Override
  public int getPriority() {
    if (status == Status.ACTIVE) {
      return phase == Phase.DONE ? -1 : this.priority;
    }
    if (StatusEffectsControllerComponent.isConcealed(target)) {
      return -1;
    }

    long now = gameTime.getTime();
    if (now < cooldownEndTime) {
      return -1;
    }
    if (getDistanceToTarget() <= TRIGGER_RANGE
        && ObstacleNavigation.isClear(owner.getEntity(), target.getPosition(), target)) {
      return this.priority;
    }
    return -1;
  }

  /**
   * Winds the lunge down because the target can no longer be seen. A dash already underway ends
   * normally, with its end event and cooldown. A telegraph that never became a dash is simply
   * abandoned: no dash ever started, so no dash end is announced and no cooldown is charged.
   */
  private void loseTarget(long now) {
    switch (phase) {
      case TELEGRAPH:
        movementComponent.setMoving(false);
        phase = Phase.DONE;
        break;
      case DASH:
        endDash(now);
        break;
      case DONE:
        break;
    }
  }

  private void beginDash(long now) {
    // The target may have stepped behind a wall during the telegraph.
    if (!ObstacleNavigation.isClear(owner.getEntity(), target.getPosition(), target)) {
      movementComponent.setMoving(false);
      phase = Phase.DONE;
      return;
    }
    // Aim the dash at a point just short of the target instead of a fixed distance, so the
    // enemy's body stops short of colliding with and jittering against the target's collider.
    Vector2 enemyPos = owner.getEntity().getPosition();
    Vector2 direction = target.getPosition().cpy().sub(enemyPos).nor();
    float distanceToTarget = enemyPos.dst(target.getPosition());
    float dashDistance = Math.min(DASH_DISTANCE, Math.max(distanceToTarget - DASH_STOP_BUFFER, 0f));
    dashTargetPoint = enemyPos.cpy().add(direction.scl(dashDistance));

    movementComponent.setMaxSpeed(new Vector2(DASH_SPEED, DASH_SPEED));
    movementComponent.setTarget(dashTargetPoint);
    movementComponent.setMoving(true);

    phase = Phase.DASH;
    phaseStartTime = now;
    owner.getEntity().getEvents().trigger("lungeDashStart");
  }

  private void endDash(long now) {
    PhysicsMovementComponent movementComponent = enemy.getComponent(PhysicsMovementComponent.class);
    movementComponent.setMoving(false);
    movementComponent.setMaxSpeed(new Vector2(restoreSpeed, restoreSpeed));

    cooldownEndTime = now + cooldownDurationMillis;
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
