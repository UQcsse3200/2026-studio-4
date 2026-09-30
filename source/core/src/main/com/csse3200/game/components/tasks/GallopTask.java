package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;

/** Task used by Sleipnir to gallop past the player in phase one * */
public class GallopTask extends DefaultTask implements PriorityTask {

  private final Vector2[] patrolPoints;
  private final Entity target;
  private int currentPoint;
  private PhysicsMovementComponent movementComponent;
  private final Vector2 targetPos;

  public GallopTask(Entity target, GridPoint2 mapBounds) {
    targetPos = target.getCenterPosition();
    patrolPoints = getPatrol(mapBounds);
    this.target = target;
  }

  private Vector2[] getPatrol(GridPoint2 mapBounds) {
    Vector2 leftTop = new Vector2(mapBounds.x - 2f, mapBounds.y - 2f);
    Vector2 RightBottom = new Vector2(mapBounds.x, mapBounds.y);
    Vector2[] grid = new Vector2[] {leftTop, targetPos, RightBottom};
    return grid;
  }

  @Override
  public void start() {
    super.start();
    movementComponent = owner.getEntity().getComponent(PhysicsMovementComponent.class);
    movementComponent.setMaxSpeed(new Vector2(2f, 2f));
    setTarget();
    movementComponent.setMoving(true);
    owner.getEntity().getEvents().trigger("gallopStart");
  }

  @Override
  public void update() {
    Vector2 position = owner.getEntity().getPosition(); // position of horse
    if (position.dst(patrolPoints[currentPoint]) <= 0.2f) { // if horse almost at points
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
