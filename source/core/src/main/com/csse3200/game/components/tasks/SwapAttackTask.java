package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.ServiceLocator;

/** Briefly charges, then swaps the enemy's position with the player. */
public class SwapAttackTask extends DefaultTask implements PriorityTask {
  static final float SWAP_RANGE = 7f;
  static final long CAST_TIME = 800L;
  static final long COOLDOWN = 7000L;

  private final Entity target;
  private long castStarted;
  private long nextCastTime;

  public SwapAttackTask(Entity target) {
    this.target = target;
  }

  @Override
  public int getPriority() {
    if (StatusEffectsControllerComponent.isConcealed(target)) {
      return -1;
    }
    if (status == Status.ACTIVE) {
      return 12;
    }
    float distance = owner.getEntity().getCenterPosition().dst(target.getCenterPosition());
    return now() >= nextCastTime && distance <= SWAP_RANGE ? 12 : -1;
  }

  @Override
  public void setPriority(int priority) {
    // This special attack always has a fixed priority.
  }

  @Override
  public void start() {
    super.start();
    castStarted = now();
    setMoving(false);
    owner.getEntity().getEvents().trigger("rangedAttack");
  }

  @Override
  public void update() {
    if (now() - castStarted < CAST_TIME) {
      return;
    }

    swapPositions(owner.getEntity(), target);
    setMoving(false);
    nextCastTime = now() + COOLDOWN;
    status = Status.FINISHED;
    owner.getEntity().getEvents().trigger("default");
  }

  @Override
  public void stop() {
    if (status == Status.ACTIVE) {
      nextCastTime = now() + COOLDOWN;
    }
    super.stop();
  }

  private void setMoving(boolean moving) {
    PhysicsMovementComponent movement =
        owner.getEntity().getComponent(PhysicsMovementComponent.class);
    if (movement != null) {
      movement.setMoving(moving);
    }
  }

  private static void swapPositions(Entity enemy, Entity player) {
    Vector2 enemyCentre = enemy.getCenterPosition();
    Vector2 playerCentre = player.getCenterPosition();
    enemy.setPosition(playerCentre.sub(enemy.getScale().scl(0.5f)));
    player.setPosition(enemyCentre.sub(player.getScale().scl(0.5f)));
  }

  private long now() {
    return ServiceLocator.getTimeSource().getTime();
  }
}
