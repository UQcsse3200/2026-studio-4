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

  public GallopTask(Entity target, GridPoint2 mapBounds, Entity entity) {
    this.target = target;
    patrolPoints = getPatrol(mapBounds);
    entity.getEvents().addListener("enragePhaseStarted", this::stop);
  }

  private Vector2[] getPatrol(GridPoint2 mapBounds) {
    Vector2 leftTop = new Vector2(0, mapBounds.y / 2f);
    Vector2 rightTop = new Vector2(mapBounds.x / 2f, mapBounds.y / 2f);
    Vector2 RightBottom = new Vector2(mapBounds.x / 2f, 0);
    Vector2 leftBottom = new Vector2(0, 0);
    return (new Vector2[] {leftTop, RightBottom, leftBottom, rightTop});
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
