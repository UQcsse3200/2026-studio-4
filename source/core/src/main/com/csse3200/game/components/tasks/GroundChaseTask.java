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
import java.util.List;

/** Ground pursuit for split enemies, retaining nearby targets while routing around obstacles. */
public class GroundChaseTask extends DefaultTask implements PriorityTask {
  private static final long REPLAN_INTERVAL_MS = 400L;
  private static final float STOP_DISTANCE = 0.1f;
  private final Entity target;
  private final int priority;
  private final float viewDistance;
  private final float maxChaseDistance;
  private final GameTime time;
  private PhysicsMovementComponent movement;
  private List<Vector2> route = List.of();
  private long nextPlanTime;

  public GroundChaseTask(Entity target, int priority, float viewDistance, float maxChaseDistance) {
    this.target = target;
    this.priority = priority;
    this.viewDistance = viewDistance;
    this.maxChaseDistance = maxChaseDistance;
    this.time = ServiceLocator.getTimeSource();
  }

  @Override
  public void start() {
    super.start();
    movement = owner.getEntity().getComponent(PhysicsMovementComponent.class);
    route = List.of();
    nextPlanTime = time.getTime();
    owner.getEntity().getEvents().trigger("chaseStart");
  }

  @Override
  public int getPriority() {
    float range = status == Status.ACTIVE ? maxChaseDistance : viewDistance;
    if (canPursue(range)) {
      return priority;
    }
    // The scheduler skips update() if every task has a negative priority.
    if (status == Status.ACTIVE) {
      movement.setMoving(false);
      route = List.of();
    }
    return -1;
  }

  @Override
  public void update() {
    Entity entity = owner.getEntity();
    if (!canPursue(maxChaseDistance)) {
      movement.setMoving(false);
      route = List.of();
      nextPlanTime = time.getTime();
      return;
    }
    Vector2 destination = target.getPosition();
    if (entity.getPosition().dst(destination) <= STOP_DISTANCE) {
      movement.setMoving(false);
      return;
    }
    if (ObstacleNavigation.isClear(entity, destination)) {
      route = List.of();
      moveTo(destination);
      return;
    }
    if (time.getTime() >= nextPlanTime) {
      route = ObstacleNavigation.findPath(entity, destination);
      nextPlanTime = time.getTime() + REPLAN_INTERVAL_MS;
    }
    followRoute(entity);
  }

  private void followRoute(Entity entity) {
    // Only skip a corner if the entire collider can reach the next waypoint from here.
    for (int i = route.size() - 1; i >= 0; i--) {
      Vector2 waypoint = route.get(i);
      if (ObstacleNavigation.isClear(entity, waypoint)) {
        route = route.subList(i, route.size());
        moveTo(waypoint);
        return;
      }
    }
    movement.setMoving(false);
  }

  private void moveTo(Vector2 point) {
    movement.setTarget(point.cpy());
    movement.setMoving(true);
  }

  private boolean canPursue(float range) {
    return !StatusEffectsControllerComponent.isConcealed(target)
        && owner.getEntity().getPosition().dst(target.getPosition()) < range;
  }

  @Override
  public void stop() {
    super.stop();
    movement.setMoving(false);
    route = List.of();
  }
}
