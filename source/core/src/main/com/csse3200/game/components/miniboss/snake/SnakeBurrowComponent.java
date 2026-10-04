package com.csse3200.game.components.miniboss.snake;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.Objects;
import java.util.Optional;

/** Owns the Snake's burrow, locked warning, single strike and vulnerable recovery cycle. */
public class SnakeBurrowComponent extends Component {
  public enum State {
    BURROWING,
    UNDERGROUND,
    WARNING,
    EXPOSED,
    DEAD
  }

  private static final float ORBIT_SPEED = 110f;
  private static final float TRAIL_INTERVAL = 0.08f;
  private static final String DEATH_ANIMATION = "dieAnimation";
  private final Entity target;
  private final SnakeMiniBossConfig config;
  private final Vector2 warningCentre = new Vector2();
  private final Vector2 warningStart = new Vector2();
  private SnakeBurrowArena arena = new SnakeBurrowArena(null);
  private CombatStatsComponent stats;
  private PhysicsComponent physics;
  private AnimationRenderComponent animator;
  private SnakeBurrowVisualComponent visual;
  private GameTime time;
  private State state = State.BURROWING;
  private float stateTime;
  private float orbitAngle;
  private float trailTime;
  private boolean started;
  private boolean disposed;
  private boolean deathPhysicsStopped;

  public SnakeBurrowComponent(Entity target, SnakeMiniBossConfig config) {
    this.target = Objects.requireNonNull(target);
    this.config = Objects.requireNonNull(config);
  }

  /** Supplies room world bounds before the encounter is registered. */
  public void setArenaBounds(Rectangle bounds) {
    arena = new SnakeBurrowArena(bounds);
  }

  @Override
  public void create() {
    stats = Objects.requireNonNull(entity.getComponent(CombatStatsComponent.class));
    physics = entity.getComponent(PhysicsComponent.class);
    animator = entity.getComponent(AnimationRenderComponent.class);
    visual = entity.getComponent(SnakeBurrowVisualComponent.class);
    time = ServiceLocator.getTimeSource();
    stats.setInvulnerable(true);
    if (animator != null) {
      animator.stopAnimation();
    }
    entity.getEvents().addListener("entityDied", this::onDeath);
  }

  @Override
  public void earlyUpdate() {
    initialiseEncounter();
  }

  private void initialiseEncounter() {
    if (started || disposed || state == State.DEAD) {
      return;
    }
    started = true;
    PhysicsMovementComponent movement = entity.getComponent(PhysicsMovementComponent.class);
    if (movement != null) {
      movement.setEnabled(false);
    }
    orbitAngle = entity.getCenterPosition().sub(target.getCenterPosition()).angleDeg();
    beginBurrowing();
  }

  @Override
  public void update() {
    if (disposed) {
      return;
    }
    if (stats.isDead() && state != State.DEAD) {
      onDeath();
    }
    if (state == State.DEAD) {
      stopDeadPhysics();
      return;
    }
    initialiseEncounter();
    stopMotion();
    if (targetIsDead()) {
      cancelAttack();
      return;
    }
    if (StatusEffectsControllerComponent.isImmobilised(entity)
        || StatusEffectsControllerComponent.isConcealed(target)) {
      return;
    }
    float delta = time == null ? 0f : time.getDeltaTime();
    if (!Float.isFinite(delta) || delta <= 0f) {
      return;
    }
    stateTime += delta;
    // Deliberately advance at most one state per update. A slow frame must not skip a warning.
    switch (state) {
      case BURROWING -> {
        if (stateTime >= config.burrowDuration) {
          enter(State.UNDERGROUND);
        }
      }
      case UNDERGROUND -> updateUnderground(delta);
      case WARNING -> updateWarning();
      case EXPOSED -> {
        if (stateTime >= config.exposedDuration) {
          beginBurrowing();
        }
      }
      default -> {
        // Death is handled before advancing encounter time.
      }
    }
  }

  private void beginBurrowing() {
    enter(State.BURROWING);
    setUnderground(true);
    if (visual != null) {
      visual.hideWarning();
      visual.startBurrow(entity.getCenterPosition());
    }
  }

  private void updateUnderground(float delta) {
    orbitAngle = (orbitAngle + ORBIT_SPEED * delta) % 360f;
    Vector2 destination =
        target
            .getCenterPosition()
            .add(
                MathUtils.cosDeg(orbitAngle) * config.orbitRadius,
                MathUtils.sinDeg(orbitAngle) * config.orbitRadius);
    destination = arena.constrain(destination, entity.getScale().scl(0.5f));
    Vector2 centre = entity.getCenterPosition();
    Vector2 offset = destination.sub(centre).limit(config.burrowSpeed * delta);
    setCentre(centre.add(offset));
    trailTime += delta;
    if (trailTime >= TRAIL_INTERVAL) {
      trailTime = 0f;
      if (visual != null) {
        visual.addTrail(entity.getCenterPosition());
      }
    }
    if (stateTime >= config.undergroundDuration) {
      beginWarning();
    }
  }

  private void beginWarning() {
    Vector2 halfSize = entity.getScale().scl(0.5f);
    halfSize.x = Math.max(halfSize.x, config.burrowAttackRadius);
    halfSize.y = Math.max(halfSize.y, config.burrowAttackRadius);
    Optional<Vector2> centre = arena.findClearCentre(target.getCenterPosition(), halfSize);
    if (centre.isEmpty()) {
      // Retry after another underground circuit instead of emerging inside a wall.
      stateTime = 0f;
      return;
    }
    warningCentre.set(centre.get());
    warningStart.set(entity.getCenterPosition());
    enter(State.WARNING);
    if (visual != null) {
      visual.showWarning(warningCentre, config.burrowAttackRadius);
    }
  }

  private void updateWarning() {
    float progress = Math.min(1f, stateTime / config.warningDuration);
    setCentre(warningStart.cpy().lerp(warningCentre, progress));
    if (stateTime >= config.warningDuration) {
      emerge();
    }
  }

  private void emerge() {
    enter(State.EXPOSED);
    setCentre(warningCentre);
    setUnderground(false);
    if (visual != null) {
      visual.hideWarning();
      visual.startEmergence(warningCentre);
    }
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (targetStats != null
        && !targetStats.isDead()
        && target.getCenterPosition().dst2(warningCentre)
            <= config.burrowAttackRadius * config.burrowAttackRadius) {
      targetStats.takeDamage(config.burrowDamage, entity);
    }
  }

  private void setUnderground(boolean underground) {
    stats.setInvulnerable(underground);
    if (physics != null) {
      physics.setEnabled(!underground);
      stopMotion();
    }
    if (animator != null) {
      if (underground) {
        animator.stopAnimation();
      } else {
        animator.startAnimation("default");
      }
    }
    entity.getEvents().trigger("enemyHealthBarVisible", !underground);
  }

  private void setCentre(Vector2 centre) {
    entity.setPosition(centre.cpy().mulAdd(entity.getScale(), -0.5f));
  }

  private void stopMotion() {
    if (physics != null) {
      physics.getBody().setLinearVelocity(0f, 0f);
    }
  }

  private boolean targetIsDead() {
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    return targetStats != null && targetStats.isDead();
  }

  private void cancelAttack() {
    enter(State.BURROWING);
    setUnderground(true);
    if (visual != null) {
      visual.clear();
    }
  }

  private void enter(State next) {
    state = next;
    stateTime = 0f;
    trailTime = 0f;
  }

  private void onDeath() {
    if (disposed || state == State.DEAD) {
      return;
    }
    enter(State.DEAD);
    if (visual != null) {
      visual.clear();
    }
    entity.getEvents().trigger("enemyHealthBarVisible", false);
    // A scripted kill can happen underground. Reveal the death frame without changing physics
    // inside a collision callback, or overriding an already-running death animation.
    if (animator != null
        && animator.hasAnimation(DEATH_ANIMATION)
        && !DEATH_ANIMATION.equals(animator.getCurrentAnimation())) {
      animator.startAnimation(DEATH_ANIMATION);
    }
  }

  private void stopDeadPhysics() {
    if (!deathPhysicsStopped) {
      deathPhysicsStopped = true;
      if (physics != null) {
        physics.setEnabled(false);
      }
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    state = State.DEAD;
    if (visual != null) {
      visual.clear();
    }
  }

  public State getState() {
    return state;
  }

  public float getStateTime() {
    return stateTime;
  }

  public Vector2 getWarningCentre() {
    return warningCentre.cpy();
  }

  public float getWarningRadius() {
    return config.burrowAttackRadius;
  }
}
