package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.physics.components.PhysicsMovementComponent;

/** Makes an enemy patrol around three points. */
public class PatrolTask extends DefaultTask implements PriorityTask {
  private static final float POINT_DISTANCE = 0.2f;

  private final Vector2[] patrolPoints;
  private PhysicsMovementComponent movementComponent;
  private int currentPoint;

  public PatrolTask(Vector2 leftPoint, Vector2 topPoint, Vector2 rightPoint) {
    patrolPoints = new Vector2[] {leftPoint.cpy(), topPoint.cpy(), rightPoint.cpy()};
  }

  @Override
  public int getPriority() {
    return 1;
  }

  @Override
  public void start() {
    super.start();
    movementComponent = owner.getEntity().getComponent(PhysicsMovementComponent.class);

    setTarget();
    movementComponent.setMoving(true);
    owner.getEntity().getEvents().trigger("patrolStart");
  }

  @Override
  public void update() {
    Vector2 position = owner.getEntity().getPosition();
    if (position.dst(patrolPoints[currentPoint]) <= POINT_DISTANCE) {
      currentPoint = (currentPoint + 1) % patrolPoints.length;
      setTarget();
    }
  }

  @Override
  public void stop() {
    super.stop();
    movementComponent.setMoving(false);
  }

  private void setTarget() {
    movementComponent.setTarget(patrolPoints[currentPoint]);
  }
}
