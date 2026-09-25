package com.csse3200.game.components.miniboss.dragon;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/** Controls cloud dash telegraph, direction locking, and recovery. */
public class DragonCloudDashComponent extends Component {
  public enum State {
    READY,
    WARNING,
    DASHING,
    RECOVERING,
    STOPPED
  }

  public static final String ATTACK_STARTED = "dragonCloudDashStarted";
  private static final float WARNING_DURATION = 0.8f;
  private static final float RECOVERY_DURATION = 1f;
  private static final float ENRAGED_RECOVERY_DURATION = 0.75f;

  private final Entity target;
  private final Vector2 lockedDirection = new Vector2();

  private CombatStatsComponent combatStats;
  private CombatStatsComponent targetStats;
  private DragonPhaseComponent phase;
  private State state = State.READY;
  private float remaining;

  public DragonCloudDashComponent(Entity target) {
    if (target == null) {
      throw new IllegalArgumentException("Target is required");
    }
    this.target = target;
  }

  @Override
  public void create() {
    combatStats = entity.getComponent(CombatStatsComponent.class);
    targetStats = target.getComponent(CombatStatsComponent.class);
    phase = entity.getComponent(DragonPhaseComponent.class);

    if (combatStats == null || targetStats == null || phase == null) {
      throw new IllegalStateException(
          "Cloud dash requires dragon and target stats, and DragonPhaseComponent");
    }

    entity.getEvents().addListener("entityDied", this::stop);
  }

  /** Starts a warning and locks the direction towards the target. */
  public boolean tryAttack() {
    if (state != State.READY
        || combatStats == null
        || combatStats.isDead()
        || targetStats == null
        || targetStats.isDead()) {
      return false;
    }

    Vector2 offset = target.getCenterPosition().sub(entity.getCenterPosition());
    if (offset.isZero(0.001f)) {
      return false;
    }

    lockedDirection.set(offset).nor();
    remaining = WARNING_DURATION;
    state = State.WARNING;
    entity.getEvents().trigger(ATTACK_STARTED);
    return true;
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  /** Advances warning and recovery timers. Movement completes the dash separately. */
  public void update(float delta) {
    if (state == State.STOPPED || combatStats == null) {
      return;
    }

    if (combatStats.isDead() || targetStats.isDead()) {
      stop();
      return;
    }

    if (!Float.isFinite(delta) || delta <= 0f) {
      return;
    }

    switch (state) {
      case WARNING:
        remaining = Math.max(0f, remaining - delta);
        if (remaining == 0f) {
          state = State.DASHING;
        }
        break;

      case RECOVERING:
        remaining = Math.max(0f, remaining - delta);
        if (remaining == 0f) {
          state = State.READY;
        }
        break;

      default:
        break;
    }
  }

  /** Called by movement when the dash reaches its limit or hits an obstacle. */
  public void finishDash() {
    if (state != State.DASHING) {
      return;
    }

    state = State.RECOVERING;
    remaining = phase.isEnraged() ? ENRAGED_RECOVERY_DURATION : RECOVERY_DURATION;
  }

  public State getState() {
    return state;
  }

  /** Returns a copy so callers cannot change the locked direction. */
  public Vector2 getLockedDirection() {
    return lockedDirection.cpy();
  }

  /** Permanently cancels the skill. */
  public void stop() {
    state = State.STOPPED;
    remaining = 0f;
    lockedDirection.setZero();
  }

  @Override
  public void dispose() {
    stop();
    super.dispose();
  }
}
