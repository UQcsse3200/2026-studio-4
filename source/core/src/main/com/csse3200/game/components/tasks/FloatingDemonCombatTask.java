package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.FloatingDemonProjectileFactory;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.physics.raycast.RaycastHit;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.Consumer;

/** Stage 3 的浮游怪：移动一会儿，停下来瞄准，再发射。 */
public class FloatingDemonCombatTask extends DefaultTask implements PriorityTask {
  private final Entity target;
  private final Consumer<Entity> spawner;
  private final float viewDistance;
  private final float chaseDistance;
  private final int circleDirection;
  private int priority = 5;
  private float cooldown;
  private float aimTime;
  private float retreatTime;
  private boolean aiming;
  private final Vector2 aimPosition = new Vector2();
  private PhysicsMovementComponent movement;

  public FloatingDemonCombatTask(
      Entity target,
      Consumer<Entity> spawner,
      float viewDistance,
      float chaseDistance,
      int summonIndex) {
    this.target = target;
    this.spawner = spawner;
    this.viewDistance = viewDistance;
    this.chaseDistance = chaseDistance;
    // 两只怪朝不同方向绕，第一次开火也错开。
    circleDirection = summonIndex % 2 == 0 ? 1 : -1;
    cooldown = 1f + summonIndex * 1.2f;
  }

  @Override
  public int getPriority() {
    if (StatusEffectsControllerComponent.isConcealed(target)) return -1;
    float distance = owner.getEntity().getCenterPosition().dst(target.getCenterPosition());
    float range = status == Status.ACTIVE ? chaseDistance : viewDistance;
    if (distance > range) return -1;
    boolean blocked =
        ServiceLocator.getPhysicsService()
            .getPhysics()
            .raycast(
                owner.getEntity().getCenterPosition(),
                target.getCenterPosition(),
                PhysicsLayer.OBSTACLE,
                new RaycastHit());
    return blocked ? -1 : priority;
  }

  @Override
  public void setPriority(int priority) {
    this.priority = priority;
  }

  @Override
  public void start() {
    super.start();
    movement = owner.getEntity().getComponent(PhysicsMovementComponent.class);
    owner.getEntity().getEvents().trigger("chaseStart");
  }

  @Override
  public void stop() {
    super.stop();
    movement.setMoving(false);
    aiming = false;
    // 玩家离开视线后，重新看见时不要马上开火。
    cooldown = Math.max(cooldown, 0.6f);
  }

  @Override
  public void update() {
    if (StatusEffectsControllerComponent.isConcealed(target)) {
      movement.setMoving(false);
      aiming = false;
      return;
    }
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (!Float.isFinite(delta) || delta <= 0f) return;

    if (aiming) {
      movement.setMoving(false);
      aimTime -= delta;
      if (aimTime <= 0f) {
        shoot();
        aiming = false;
        cooldown = 2.4f;
        retreatTime = 0f;
      }
      return;
    }

    cooldown -= delta;
    Vector2 direction = target.getCenterPosition().sub(owner.getEntity().getCenterPosition());
    float distance = direction.len();
    if (distance <= 6f && cooldown <= 0f) {
      // 记住现在的位置，玩家在这 0.4 秒里可以躲开。
      aimPosition.set(target.getCenterPosition());
      aiming = true;
      aimTime = 0.4f;
      movement.setMoving(false);
      owner.getEntity().getEvents().trigger("rangedAttack");
      return;
    }

    direction.nor();
    if (distance < 3f && retreatTime < 0.6f) {
      // 靠太近就退一点，但不能一直往后逃。
      direction.scl(-1f);
      retreatTime += delta;
    } else if (distance <= 6f) {
      direction.rotateDeg(90f * circleDirection);
    }
    movement.setTarget(owner.getEntity().getPosition().add(direction));
    movement.setMoving(true);
  }

  private void shoot() {
    Entity demon = owner.getEntity();
    Vector2 position = demon.getCenterPosition();
    Vector2 direction = aimPosition.cpy().sub(position).nor();
    int damage = demon.getComponent(CombatStatsComponent.class).getBaseAttack();
    for (int angle = -10; angle <= 10; angle += 10) {
      Vector2 shotDirection = direction.cpy().rotateDeg(angle);
      ServiceLocator.getEntityService()
          .runAfterUpdate(
              () -> {
                // 这一帧内怪物可能刚被打死，死了就不用发射了。
                if (demon.getComponent(CombatStatsComponent.class).isDead()
                    || StatusEffectsControllerComponent.isConcealed(target)) return;
                spawner.accept(
                    FloatingDemonProjectileFactory.createProjectile(
                        position, shotDirection, damage));
              });
    }
  }
}
