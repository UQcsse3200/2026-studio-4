package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Used by Sleipnir in phase two, miniboss runs around player dealing constant damage, with cool
 * downs
 */
public class StampedeTask extends DefaultTask implements PriorityTask {

  private static final float RADIUS = 3f;
  private static final float SPEED = 2.5f;
  private static final float DAMAGE = 0.25f;
  private final Entity target;
  private PhysicsComponent physicsComponent;
  private boolean phaseTwoActivated;
  private float angle;
  private int rotations = 0;
  private float damageTimer;
  private Entity entity;

  public StampedeTask(Entity target, Entity entity) {
    this.target = target;
    this.entity = entity;
    entity.getEvents().addListener("enragePhaseStarted", this::activateStampede);
  }

  @Override
  public void start() {
    super.start();
    physicsComponent = owner.getEntity().getComponent(PhysicsComponent.class);
    Body body = physicsComponent.getBody();
    Vector2 horsePosition = body.getPosition();
    Vector2 targetPosition = target.getPosition();
    angle =
        (float) Math.atan2(horsePosition.y - targetPosition.y, horsePosition.x - targetPosition.x);
    phaseTwoActivated = true;
  }

  private void activateStampede() {
    phaseTwoActivated = true;
  }

  @Override
  public void update() {
    float deltaTime = ServiceLocator.getTimeSource().getDeltaTime();
    if (!phaseTwoActivated) {
      return;
    }
    Body body = physicsComponent.getBody();
    angle += SPEED * deltaTime; // increase angle to continue moving
    Vector2 targetPosition = target.getPosition();
    Vector2 desiredPosition =
        new Vector2(
            targetPosition.x + RADIUS * (float) Math.cos(angle),
            targetPosition.y + RADIUS * (float) Math.sin(angle));

    Vector2 currentPosition = body.getPosition();
    Vector2 movementDirection = desiredPosition.sub(currentPosition);
    entity.getEvents().trigger("moving", movementDirection);
    body.setLinearVelocity(movementDirection.scl(1f / deltaTime));
    applyDamage(deltaTime);
  }

  /**
   * attack player with damage of 1
   *
   * @param deltaTime the time at update since last update
   */
  private void applyDamage(float deltaTime) {
    damageTimer -= deltaTime;
    if (damageTimer > 0f) {
      return;
    }
    float distance = owner.getEntity().getPosition().dst(target.getPosition());

    if (distance <= RADIUS + 0.5f) { // if within range deal damage to player
      target.getComponent(CombatStatsComponent.class).takeDamage(1, owner.getEntity());
      damageTimer = DAMAGE;
      rotations += 1;
    }
  }

  @Override
  public void stop() {
    super.stop();
    physicsComponent.getBody().setLinearVelocity(0f, 0f);
    phaseTwoActivated = false;
  }

  @Override
  public int getPriority() {
    if (phaseTwoActivated) {
      return 15;
    } else {
      return -10;
    }
  }

  @Override
  public void setPriority(int priority) {}
}
