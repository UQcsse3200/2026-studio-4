package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;

/** Task used by Sleipnir to run in a square pattern, used alongside OneAttackTask in phase one * */
public class GallopTask extends DefaultTask implements PriorityTask {

  private final Vector2[] patrolPoints;
  private final Entity target;
  private int currentPoint;
  private PhysicsMovementComponent movementComponent;

  public GallopTask(Entity target, Vector2[] mapBounds) {
    this.target = target;
    patrolPoints = createBounds(mapBounds);
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
  public void start() {
    super.start();
    movementComponent = owner.getEntity().getComponent(PhysicsMovementComponent.class);
    setTarget();
    movementComponent.setMoving(true);
  }

  @Override
  public void update() {
    Vector2 position = owner.getEntity().getPosition(); // current position of horse
    if (position.dst(patrolPoints[currentPoint]) <= 2f) { // if horse almost at points
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

  @Override
  public int getPriority() {
    return 2;
  }

  @Override
  public void setPriority(int status) {}
}
