package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.physics.components.PhysicsMovementComponent;

/** Makes an enemy patrol left and right between two map edges. */
public class PatrolTask extends DefaultTask implements PriorityTask {
  private static final float EDGE_DISTANCE = 0.1f;

  private final float leftEdge;
  private final float rightEdge;
  private PhysicsMovementComponent movementComponent;
  private boolean movingRight = true;

  public PatrolTask(float leftEdge, float rightEdge) {
    this.leftEdge = leftEdge;
    this.rightEdge = rightEdge;
  }

  @Override
  public int getPriority() {
    return 1;
  }

  @Override
  public void start() {
    super.start();
    movementComponent = owner.getEntity().getComponent(PhysicsMovementComponent.class);

    // Start by travelling towards the right side of the map.
    movingRight = true;
    setTarget(rightEdge);
    movementComponent.setMoving(true);
    owner.getEntity().getEvents().trigger("patrolStart");
  }

  @Override
  public void update() {
    float currentX = owner.getEntity().getPosition().x;

    if (movingRight && currentX >= rightEdge - EDGE_DISTANCE) {
      movingRight = false;
      setTarget(leftEdge);
    } else if (!movingRight && currentX <= leftEdge + EDGE_DISTANCE) {
      movingRight = true;
      setTarget(rightEdge);
    }
  }

  @Override
  public void stop() {
    super.stop();
    movementComponent.setMoving(false);
  }

  private void setTarget(float targetX) {
    float currentY = owner.getEntity().getPosition().y;
    movementComponent.setTarget(new Vector2(targetX, currentY));
  }
}
