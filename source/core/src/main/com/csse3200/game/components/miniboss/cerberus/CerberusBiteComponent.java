package com.csse3200.game.components.miniboss.cerberus;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.ServiceLocator;

public class CerberusBiteComponent extends Component {
  private enum State {
    READY,
    WINDUP,
    LUNGE,
    COOLDOWN
  }

  private static final float TRIGGER_RANGE = 2f;
  private static final float HIT_RANGE = 1.2f;
  private static final float WINDUP_TIME = 0.5f;
  private static final float LUNGE_TIME = 0.35f;
  private static final float COOLDOWN_TIME = 2f;
  private static final float LUNGE_SPEED = 6f;

  private final Entity target;
  private final Vector2 anchor;
  private final float radius;

  private CombatStatsComponent combatStats;
  private CombatStatsComponent targetStats;
  private State state = State.READY;
  private Vector2 destination;
  private float remaining;
  private boolean hit;
  private CerberusAttackCoordinator attackCoordinator;

  public CerberusBiteComponent(Entity target, Vector2 anchor, float radius) {
    this.target = target;
    this.anchor = anchor.cpy();
    this.radius = radius;
  }

  @Override
  public void create() {
    combatStats = entity.getComponent(CombatStatsComponent.class);
    targetStats = target.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("entityDied", this::cancel);
  }

  @Override
  public void update() {
    if (combatStats.isDead() || targetStats == null || targetStats.isDead()) {
      cancel();
      return;
    }
    float delta = Math.max(0f, ServiceLocator.getTimeSource().getDeltaTime());

    switch (state) {
      case READY:
        if (canStartAttack() && (attackCoordinator == null || attackCoordinator.tryStart(entity))) {
          destination =
              target.getPosition().sub(anchor).limit(Math.max(0f, radius - 0.5f)).add(anchor);
          hit = false;
          state = State.WINDUP;
          remaining = WINDUP_TIME;
          entity.getEvents().trigger("biteWindup");
        }
        break;

      case WINDUP:
        remaining -= delta;
        if (remaining <= 0f) {
          state = State.LUNGE;
          remaining = LUNGE_TIME;
          entity.getEvents().trigger("attackStart");
        }
        break;

      case LUNGE:
        if (!hit && inRange(HIT_RANGE)) {
          hit = true;
          targetStats.takeDamage(combatStats.getBaseAttack(), entity);
        }

        remaining -= delta;
        if (remaining <= 0f) {
          state = State.COOLDOWN;
          remaining = COOLDOWN_TIME;
          finishAttack();
          entity.getEvents().trigger("default");
        }
        break;

      case COOLDOWN:
        remaining -= delta;
        if (remaining <= 0f) {
          state = State.READY;
        }
        break;
    }
  }

  /**
   * Applies attack movement before normal pursuit.
   *
   * @return true when the attack owns movement for this frame
   */
  public boolean controlMovement(PhysicsMovementComponent movement) {
    if (combatStats.isDead()) {
      movement.setMoving(false);
      return true;
    }
    if (state == State.WINDUP) {
      movement.setMoving(false);
      return true;
    }

    if (state == State.LUNGE) {
      movement.setMaxSpeed(new Vector2(LUNGE_SPEED, LUNGE_SPEED));
      movement.setTarget(destination.cpy());
      movement.setMoving(entity.getPosition().dst2(destination) > 0.04f);
      return true;
    }

    return false;
  }

  private boolean inRange(float range) {
    return entity.getCenterPosition().dst2(target.getCenterPosition()) <= range * range;
  }

  private void cancel() {
    state = State.READY;
    remaining = 0f;
    destination = null;
    hit = false;
    finishAttack();
  }

  @Override
  public void dispose() {
    cancel();
    super.dispose();
  }

  /** Connects this skill after it has been added to its head entity. */
  public void setAttackCoordinator(CerberusAttackCoordinator coordinator) {
    attackCoordinator = coordinator;
    coordinator.register(entity, this::canStartAttack);
  }

  private boolean canStartAttack() {
    return combatStats != null
        && !combatStats.isDead()
        && targetStats != null
        && !targetStats.isDead()
        && state == State.READY
        && inRange(TRIGGER_RANGE);
  }

  private void finishAttack() {
    if (attackCoordinator != null) {
      attackCoordinator.finish(entity);
    }
  }
}
