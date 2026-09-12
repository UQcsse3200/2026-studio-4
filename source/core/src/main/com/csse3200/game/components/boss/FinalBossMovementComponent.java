package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Controls the movement modes used by Grandpa during Stage 1. */
public class FinalBossMovementComponent extends Component {
  /** Available Stage 1 movement modes. */
  public enum Mode {
    STOPPED,
    WANDER_AVOID_SUMMONS,
    FLEE_PLAYER
  }

  private final Entity target;
  private final FinalBossStageOneConfig config;
  private final Set<Entity> activeSummons = new HashSet<>();

  private PhysicsMovementComponent movement;
  private Mode mode = Mode.STOPPED;
  private float refreshRemaining;
  private float wanderAngle;

  public FinalBossMovementComponent(Entity target, FinalBossStageOneConfig config) {
    if (target == null || config == null) {
      throw new IllegalArgumentException("Target and config must not be null");
    }

    this.target = target;
    this.config = config;
  }

  @Override
  public void create() {
    movement = entity.getComponent(PhysicsMovementComponent.class);

    if (movement == null) {
      throw new IllegalStateException(
          "FinalBossMovementComponent requires PhysicsMovementComponent");
    }
  }

  @Override
  public void update() {
    if (mode == Mode.STOPPED
        || (mode == Mode.FLEE_PLAYER && PlayerAbilitiesComponent.isInvisible(target))) {
      movement.setMoving(false);
      refreshRemaining = 0f;
      return;
    }

    movement.setMoving(true);

    GameTime time = ServiceLocator.getTimeSource();
    float deltaTime = time == null ? 0f : Math.max(time.getDeltaTime(), 0f);

    refreshRemaining -= deltaTime;

    if (refreshRemaining > 0f) {
      return;
    }

    refreshRemaining = config.movementTargetRefreshInterval;

    if (mode == Mode.FLEE_PLAYER) {
      updateFleeTarget();
    } else {
      updateWanderTarget();
    }
  }

  /** Changes the active movement behaviour. */
  public void setMode(Mode mode) {
    if (mode == null) {
      throw new IllegalArgumentException("Movement mode must not be null");
    }

    this.mode = mode;
    refreshRemaining = 0f;

    PhysicsMovementComponent controller = requireMovement();

    float speed = mode == Mode.FLEE_PLAYER ? config.bossFleeSpeed : config.bossWanderSpeed;

    controller.setMaxSpeed(new Vector2(speed, speed));

    if (mode == Mode.STOPPED) {
      controller.setMoving(false);
    }
  }

  public Mode getMode() {
    return mode;
  }

  /** Updates the summons considered by the avoidance calculation. */
  public void setActiveSummons(Collection<Entity> summons) {
    activeSummons.clear();

    if (summons != null) {
      activeSummons.addAll(summons);
    }
  }

  private void updateFleeTarget() {
    Vector2 direction = entity.getCenterPosition().sub(target.getCenterPosition());

    if (direction.isZero()) {
      direction.set(1f, 0f);
    }

    Vector2 destination =
        entity.getPosition().mulAdd(direction.nor(), config.movementTargetDistance);

    movement.setTarget(clampToVisibleArea(destination));
  }

  private void updateWanderTarget() {
    Vector2 bossPosition = entity.getCenterPosition();
    Vector2 crowdedCentre = new Vector2();
    int nearbySummons = 0;

    for (Entity summon : activeSummons) {
      Vector2 summonPosition = summon.getCenterPosition();

      if (bossPosition.dst(summonPosition) <= config.summonAvoidanceRadius) {
        crowdedCentre.add(summonPosition);
        nearbySummons++;
      }
    }

    Vector2 direction;

    if (nearbySummons > 0) {
      crowdedCentre.scl(1f / nearbySummons);
      direction = bossPosition.sub(crowdedCentre);

      if (direction.isZero()) {
        direction.set(1f, 0f);
      }
    } else {
      wanderAngle = (wanderAngle + 137f) % 360f;

      direction = new Vector2(1f, 0f).setAngleDeg(wanderAngle);
    }

    Vector2 destination =
        entity.getPosition().mulAdd(direction.nor(), config.movementTargetDistance);

    movement.setTarget(clampToVisibleArea(destination));
  }

  /**
   * Restricts the Boss to a conservative visible area around the player.
   *
   * <p>The current game camera follows the player. The Boss scale is included so the entire sprite,
   * rather than only its centre, remains visible.
   */
  private Vector2 clampToVisibleArea(Vector2 destination) {
    Vector2 visibleCentre = target.getCenterPosition();
    Vector2 halfBossSize = entity.getScale().scl(0.5f);
    Vector2 desiredCentre = destination.cpy().add(halfBossSize);

    float minimumX = visibleCentre.x - config.bossVisibilityHalfWidth + halfBossSize.x;
    float maximumX = visibleCentre.x + config.bossVisibilityHalfWidth - halfBossSize.x;
    float minimumY = visibleCentre.y - config.bossVisibilityHalfHeight + halfBossSize.y;
    float maximumY = visibleCentre.y + config.bossVisibilityHalfHeight - halfBossSize.y;

    desiredCentre.x = MathUtils.clamp(desiredCentre.x, minimumX, maximumX);
    desiredCentre.y = MathUtils.clamp(desiredCentre.y, minimumY, maximumY);

    return desiredCentre.sub(halfBossSize);
  }

  private PhysicsMovementComponent requireMovement() {
    if (movement == null) {
      movement = entity.getComponent(PhysicsMovementComponent.class);
    }

    if (movement == null) {
      throw new IllegalStateException(
          "FinalBossMovementComponent requires PhysicsMovementComponent");
    }

    return movement;
  }
}
