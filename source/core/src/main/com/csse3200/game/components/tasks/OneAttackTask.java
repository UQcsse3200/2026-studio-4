package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;

/**
 * Task used by Sleipnir to attack player once by running past, mostly in phase one, after damaging
 * player speed increases
 */
public class OneAttackTask extends DefaultTask implements PriorityTask {

  private final Entity target;
  private MovementTask movementTask;
  private final float attackDist;
  private Vector2 maxSpeed;
  private boolean hit = false;
  private final float coolDownDist;
  private final float MAX_SPEED = 10f;

  public OneAttackTask(Entity target, float attackDist, float coolDown, Vector2 maxSpeed) {
    this.target = target;
    this.attackDist = attackDist;
    this.maxSpeed = maxSpeed;
    this.coolDownDist = coolDown;
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
    PhysicsMovementComponent physicsMovementComponent =
        owner.getEntity().getComponent(PhysicsMovementComponent.class);
    Vector2 increasedSpeed = (new Vector2(maxSpeed.x + 0.5f, maxSpeed.y + 0.5f));
    if (StatusEffectsControllerComponent.isConcealed(target)) {
      movementTask.stop();
      return;
    }
    Vector2 position = owner.getEntity().getPosition();

    if (!hit
        && (position.dst(target.getPosition())
            <= attackDist)) { // when not hit and within range of player
      movementTask.setTarget(target.getPosition());
      if (movementTask.getStatus() != Status.ACTIVE) {
        movementTask.start();
      }
      if (maxSpeed.x < MAX_SPEED) {
        this.maxSpeed = increasedSpeed;
        physicsMovementComponent.setMaxSpeed(
            increasedSpeed); // increase speed every hit until MAX_SPEED is reached
      }
      movementTask.update();
      target
          .getComponent(CombatStatsComponent.class)
          .takeDamage(combatStatsComponent.getBaseAttack(), owner.getEntity());
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
    } else if ((position.dst(target.getPosition()) > coolDownDist)) {
      this.hit = false;
      return -1;
    } else {
      return -1;
    }
  }
}
