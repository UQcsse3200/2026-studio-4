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
  private boolean flying;
  private Vector2 spawnPoint;

  public PatrolTask(Vector2 spawnPoint, boolean flying) {
    this.flying = flying;
    this.spawnPoint = spawnPoint;
  }

  private void create() {
    if (flying) {
      patrolPoints = createTriangularPatrol(spawnPoint);
      pointDist = 0.2f;
    } else {
      patrolPoints = createBounds(spawnPoint);
      pointDist = 2f;
    }
  }

  private Vector2[] createTriangularPatrol(Vector2 spawnPoint) {
    int x = (int) spawnPoint.x;
    int y = (int) spawnPoint.y;
    Vector2 leftPoint = new Vector2(x - 2, y);
    Vector2 topPoint = new Vector2(x, y + 1.5f);
    Vector2 rightPoint = new Vector2(x + 2, y);
    return new Vector2[] {leftPoint, topPoint, rightPoint};
  }

  /**
   * a function to create the four points the norse miniboss goes to with
   *
   * @return each point the miniboss patrols in order
   */
  private Vector2[] createBounds(Vector2 spawnPoint) {
    int extra = 5;
    Vector2 bottomLeft = spawnPoint;
    Vector2 bottomRight = new Vector2(spawnPoint.x + extra, spawnPoint.y);
    Vector2 topLeft = new Vector2(spawnPoint.x, spawnPoint.y + extra);
    Vector2 topRight = new Vector2(spawnPoint.x + extra, spawnPoint.y + extra);
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
    if (flying || phaseTwoActivated) {
      return PRIORITY;
    } else {
      return 1;
    }
  }

  @Override
  public void start() {
    super.start();
    movementComponent = owner.getEntity().getComponent(PhysicsMovementComponent.class);
    if (this.patrolPoints == null) {
      create();
    }
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
