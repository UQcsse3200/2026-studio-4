package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import java.awt.*;

public class OneAttackTask extends DefaultTask implements PriorityTask {

  private final Entity target;
  private MovementTask movementTask;
  private final float attackDist;
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
    CombatStatsComponent combatStatsComponent =
        owner.getEntity().getComponent(CombatStatsComponent.class);
    if (StatusEffectsControllerComponent.isConcealed(target)) {
      movementTask.stop();
      return;
    }
    Vector2 position = owner.getEntity().getPosition(); // current position of horse
    if (!hit && (position.dst(target.getPosition()) <= attackDist)) {
      movementTask.setTarget(target.getPosition());
      if (movementTask.getStatus() != Status.ACTIVE) {
        movementTask.start();
      }
      movementTask.update();
      target
          .getComponent(CombatStatsComponent.class)
          .takeDamage(combatStatsComponent.getBaseAttack());
      // trigger attack animation
      updateHit();
    }
  }

  /** set Hit to true when sleipnir attacked/ tried to attack player */
  private void updateHit() {
    this.hit = true;
  }

  @Override
  public void stop() {
    super.stop();
    movementTask.stop();
  }

  @Override
  public int getPriority() {
    Vector2 position = owner.getEntity().getPosition(); // current position of horse
    int priority = 10;
    if (StatusEffectsControllerComponent.isConcealed(target)) {
      // The scheduler may leave the current task running when all priorities are negative.
      if (status == Status.ACTIVE) {
        movementTask.stop();
      }
      return -1;
    }
    if (!hit && (position.dst(target.getPosition()) <= attackDist)) {
      return priority;
    } else if ((position.dst(target.getPosition()) > 7f)) {
      this.hit = false;
      return -1;
    } else {
      return -1;
    }
  }
}
