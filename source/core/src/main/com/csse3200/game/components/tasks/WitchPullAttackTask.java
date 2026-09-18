package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.weapons.SweepComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.ServiceLocator;

/** Witch pull player to itself and spin attack if player become close. */
public class WitchPullAttackTask extends DefaultTask implements PriorityTask {
  static final float PULL_RANGE = 8f;
  static final float MELEE_RANGE = 1.8f;
  static final float PULL_SPEED = 2.4f;
  static final long PULL_TIME = 2500L;
  static final long PULL_COOLDOWN = 7000L;
  private static final long MELEE_COOLDOWN = 900L;
  private static final float MELEE_LIFETIME = 0.65f;

  private final Entity target;
  private final int damage;
  private long pullStarted;
  private long nextPullTime;
  private long lastMeleeTime = -MELEE_COOLDOWN;

  public WitchPullAttackTask(Entity target, int damage) {
    this.target = target;
    this.damage = damage;
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
    return now() >= nextPullTime && distance <= PULL_RANGE ? 12 : -1;
  }

  @Override
  public void setPriority(int priority) {
    // This attack have fixed high priority because it is special move.
  }

  @Override
  public void start() {
    super.start();
    pullStarted = now();
    PhysicsMovementComponent movement =
        owner.getEntity().getComponent(PhysicsMovementComponent.class);
    if (movement != null) {
      movement.setMoving(false);
    }
    owner.getEntity().getEvents().trigger("witchPullStart");
  }

  @Override
  public void update() {
    long currentTime = now();
    if (currentTime - pullStarted >= PULL_TIME) {
      nextPullTime = currentTime + PULL_COOLDOWN;
      status = Status.FINISHED;
      owner.getEntity().getEvents().trigger("witchPullStop");
      return;
    }

    pullPlayer();
    float distance = owner.getEntity().getCenterPosition().dst(target.getCenterPosition());
    if (distance <= MELEE_RANGE && currentTime - lastMeleeTime >= MELEE_COOLDOWN) {
      spinAttack();
      lastMeleeTime = currentTime;
    }
  }

  @Override
  public void stop() {
    if (status == Status.ACTIVE) {
      nextPullTime = now() + PULL_COOLDOWN;
      owner.getEntity().getEvents().trigger("witchPullStop");
    }
    super.stop();
  }

  private void pullPlayer() {
    PhysicsComponent physics = target.getComponent(PhysicsComponent.class);
    if (physics == null) {
      return;
    }

    Vector2 toWitch = owner.getEntity().getCenterPosition().sub(target.getCenterPosition());
    if (toWitch.isZero(0.05f)) {
      return;
    }

    Body body = physics.getBody();
    Vector2 velocity = body.getLinearVelocity().cpy().add(toWitch.setLength(PULL_SPEED));
    // Limit it so moving opposite still can escape, just very slow.
    if (velocity.len() > 3.5f) {
      velocity.setLength(3.5f);
    }
    body.setLinearVelocity(velocity);
  }

  private void spinAttack() {
    Entity witch = owner.getEntity();
    float startAngle = -180f;
    float radius = 1.05f;
    Vector2 size = new Vector2(1f, 0.45f);

    HitboxSpec spec =
        new HitboxSpec()
            .position(witch.getCenterPosition())
            .size(size)
            .lifetime(MELEE_LIFETIME)
            .layer(PhysicsLayer.WEAPON)
            .targetLayer(PhysicsLayer.PLAYER)
            .damage(damage)
            .owner(witch)
            .localOffset(new Vector2(radius, 0f).setAngleDeg(startAngle));

    Entity hitbox = HitboxFactory.createHitbox(spec);
    // Reuse sword sweep, only no sword picture because witch uses magic.
    hitbox.addComponent(new SweepComponent(MELEE_LIFETIME, startAngle, 180f, radius));
    ServiceLocator.getEntityService().register(hitbox);
    witch.getEvents().trigger("witchMeleeAttack");
  }

  private long now() {
    return ServiceLocator.getTimeSource().getTime();
  }
}
