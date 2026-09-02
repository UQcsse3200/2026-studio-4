package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.FloatingDemonProjectileFactory;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.ServiceLocator;

/** Makes the floating demon fire three projectiles when the player is close. */
public class RangedAttackTask extends DefaultTask implements PriorityTask {
  private static final int ATTACK_PRIORITY = 5;
  private static final float ATTACK_RANGE = 6f;
  private static final float EXIT_RANGE = 7f;
  private static final float ATTACK_COOLDOWN = 1.8f;

  private final Entity target;
  private final int damage;

  private float cooldownLeft;

  public RangedAttackTask(Entity target, int damage) {
    this.target = target;
    this.damage = damage;
  }

  @Override
  public int getPriority() {
    float distance = owner.getEntity().getPosition().dst(target.getPosition());

    if (status == Status.ACTIVE && distance <= EXIT_RANGE) {
      return ATTACK_PRIORITY;
    }
    if (status != Status.ACTIVE && distance <= ATTACK_RANGE) {
      return ATTACK_PRIORITY;
    }
    return -1;
  }

  @Override
  public void start() {
    super.start();
    PhysicsMovementComponent movementComponent =
        owner.getEntity().getComponent(PhysicsMovementComponent.class);
    movementComponent.setMoving(false);
    cooldownLeft = 0f;
  }

  @Override
  public void update() {
    cooldownLeft -= ServiceLocator.getTimeSource().getDeltaTime();

    if (cooldownLeft <= 0f) {
      shootProjectiles();
      cooldownLeft = ATTACK_COOLDOWN;
    }
  }

  private void shootProjectiles() {
    Vector2 startPosition = owner.getEntity().getCenterPosition();
    Vector2 direction = target.getCenterPosition().sub(startPosition).nor();

    createProjectile(startPosition, direction.cpy().rotateDeg(10f));
    createProjectile(startPosition, direction);
    createProjectile(startPosition, direction.cpy().rotateDeg(-10f));

    owner.getEntity().getEvents().trigger("rangedAttack");
  }

  private void createProjectile(Vector2 position, Vector2 direction) {
    ServiceLocator.getEntityService()
        .runAfterUpdate(
            () -> {
              Entity projectile =
                  FloatingDemonProjectileFactory.createProjectile(position, direction, damage);
              ServiceLocator.getEntityService().register(projectile);
            });
  }
}
