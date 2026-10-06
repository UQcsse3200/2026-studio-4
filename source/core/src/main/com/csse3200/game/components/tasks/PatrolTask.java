package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.physics.components.PhysicsMovementComponent;

/**
 * Makes an enemy patrol around three points. Makes miniboss patrol 4 points with given left bottom
 * point and the required height and width
 */
public class PatrolTask extends DefaultTask implements PriorityTask {
  private float pointDist;
  private Vector2[] patrolPoints;
  private PhysicsMovementComponent movementComponent;
  private int currentPoint;
  private static final int PRIORITY = 5;
  private boolean phaseTwoActivated = false;

  public PatrolTask(Vector2[] layout) {
    if (layout.length == 3) { // flying enemy
      phaseTwoActivated = true;
      patrolPoints = layout;
      pointDist = 0.2f;
    } else if (layout.length == 2) { // norse miniboss
      pointDist = 2f;
      patrolPoints = createBounds(layout);
    }
  }

  /**
   * a function to create the four points the norse miniboss goes to with
   *
   * @param grid the first Vector2 is the bottom left point of the square and the second point is
   *     the width & height
   * @return each point the miniboss patrols in order
   */
  private Vector2[] createBounds(Vector2[] grid) {
    float width = grid[1].x;
    float height = grid[1].y;
    Vector2 bottomLeft = grid[0];
    Vector2 bottomRight = new Vector2(grid[0].x + width, grid[0].y);
    Vector2 topLeft = new Vector2(grid[0].x, grid[0].y + height);
    Vector2 topRight = new Vector2(grid[0].x + width, grid[0].y + height);
    return (new Vector2[] {bottomLeft, topRight, topLeft, bottomRight});
  }

  /**
   * update the direction the miniboss is facing - triggered and listened in
   * EnemyAnimationController
   *
   * @param currentPos the current x and y coordinates
   * @param newTarget the next x and y coordinates (both found in patrolPoints)
   */
  private void updateDirection(Vector2 currentPos, Vector2 newTarget) {
    float x = 0;
    float y = 0;
    if (currentPos.x > newTarget.x) {
      x = -1f;
    } else if (currentPos.x < newTarget.x) {
      x = 1f;
    }

    if (currentPos.y > newTarget.y) {
      y = -1f;
    } else if (currentPos.y < newTarget.y) {
      y = 1f;
    }
    owner.getEntity().getEvents().trigger("moving", new Vector2(x, y));
  }

  @Override
  public int getPriority() {
    if (phaseTwoActivated) {
      return 1;
    } else {
      return PRIORITY;
    }
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
    int temp = currentPoint;
    if (position.dst(patrolPoints[currentPoint]) <= pointDist) {
      currentPoint = (currentPoint + 1) % patrolPoints.length;
      updateDirection(patrolPoints[temp], patrolPoints[currentPoint]);
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
