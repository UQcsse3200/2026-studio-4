package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;

public class OneAttackTask extends DefaultTask implements PriorityTask {

  private Entity target;
  private MovementTask movementTask;
  private float attackDist;
  private int priority = 10;
  private boolean hit = false;

  public OneAttackTask(Entity target, float attackDist, Entity entity) {
    this.target = target;
    this.attackDist = attackDist;
    entity.getEvents().addListener("enragePhaseStarted", this::stop);
  }

  @Override
  public void setPriority(int status) {}

  @Override
  public void start() {
    super.start();
    movementTask = new MovementTask(target.getPosition());
    movementTask.create(owner);
    movementTask.start();
  }

  @Override
  public void update() {
    if (StatusEffectsControllerComponent.isConcealed(target)) {
      movementTask.stop();
      return;
    }
    Vector2 position = owner.getEntity().getPosition(); // current position of horse
    if (!hit && position.dst(target.getPosition()) <= attackDist) {
      movementTask.setTarget(target.getPosition());
      if (movementTask.getStatus() != Status.ACTIVE) {
        movementTask.start();
      }
      movementTask.update();
      hit = true;
    } else {
      hit = false;
    }
  }

  @Override
  public void stop() {
    super.stop();
    movementTask.stop();
  }

  @Override
  public int getPriority() {
    if (StatusEffectsControllerComponent.isConcealed(target)) {
      // The scheduler may leave the current task running when all priorities are negative.
      if (status == Status.ACTIVE) {
        movementTask.stop();
      }
      return -1;
    }
    if (!hit) {
      return priority;
    } else {
      return -1;
    }
  }
}
