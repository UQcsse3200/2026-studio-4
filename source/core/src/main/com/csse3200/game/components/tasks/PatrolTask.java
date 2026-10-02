package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.physics.components.PhysicsMovementComponent;

/** Makes an enemy patrol around three points. */
public class PatrolTask extends DefaultTask implements PriorityTask {
  private static float POINT_DISTANCE;

  private final Vector2[] patrolPoints;
  private PhysicsMovementComponent movementComponent;
  private int currentPoint;
  private int priority = 2;

  public PatrolTask(Vector2[] layout) {
    patrolPoints = setPatrolPoints(layout);
  }

  private Vector2[] setPatrolPoints(Vector2[] grid) {
    if (grid.length == 3) { // used by flying enemy
      POINT_DISTANCE = 0.2f;
      return grid;
    } else { // used only by norse miniboss
      POINT_DISTANCE = 2f;
      return createBounds(grid);
    }
  }

  private Vector2[] createBounds(Vector2[] grid) {
    float Xsize = grid[1].x;
    float Ysize = grid[1].y;
    Vector2 bottomLeft = grid[0];
    Vector2 bottomRight = new Vector2(grid[0].x + Xsize, grid[0].y);
    Vector2 topLeft = new Vector2(grid[0].x, grid[0].y + Ysize);
    Vector2 topRight = new Vector2(grid[0].x + Xsize, grid[0].y + Ysize);
    return (new Vector2[] {bottomLeft, topRight, topLeft, bottomRight});
  }

  @Override
  public int getPriority() {
    return priority;
  }

  @Override
  public void setPriority(int status) {
    this.priority = status;
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
