package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/** Moves Grandpa towards the player in discrete steps separated by pauses. */
public class FinalBossMovementComponent extends Component {
  /** Available Stage 1 movement modes. */
  public enum Mode {
    STOPPED,
    STEP_TOWARDS_PLAYER,
    FLEE_ALONG_EDGE
  }

  private static final float ARRIVAL_DISTANCE = 0.1f;
  private static final float SCREEN_MARGIN = 0.25f;
  private static final float PLAYER_STOP_DISTANCE = 0.75f;
  private static final float MINIMUM_STEP_TIMEOUT = 2f;

  private final Entity target;
  private final FinalBossStageOneConfig config;

  private Collection<Entity> activeSummons = Collections.emptyList();

  private PhysicsMovementComponent movement;
  private Camera camera;
  private Mode mode = Mode.STOPPED;
  private Vector2 destination;

  private float pauseRemaining;
  private float stepRemaining;
  private boolean disposed;

  /** Uses the live wave collection so removed summons are no longer considered. */
  public void setActiveSummons(Collection<Entity> summons) {
    activeSummons = summons == null ? Collections.emptyList() : summons;
  }

  public FinalBossMovementComponent(Entity target, FinalBossStageOneConfig config) {
    if (target == null || config == null) {
      throw new IllegalArgumentException("Target and config must not be null");
    }

    config.validate();
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

  /** Supplies the world camera used to constrain movement destinations. */
  public void setCamera(Camera camera) {
    this.camera = camera;
  }

  public Camera getCamera() {
    return camera;
  }

  /** Changes behaviour and resets any previous movement or pause. */
  public void setMode(Mode mode) {
    if (mode == Mode.FLEE_ALONG_EDGE && camera == null) {
      throw new IllegalStateException("Edge movement requires the world camera");
    }

    this.mode = mode;
    destination = null;
    pauseRemaining = 0f;
    stepRemaining = 0f;
  }

  public Mode getMode() {
    return mode;
  }

  @Override
  public void update() {
    if (disposed) {
      return;
    }

    // Apply during movement and pauses, including the vulnerability window.
    keepInsideVisibleArea();

    if (mode == Mode.STOPPED) {
      movement.setMoving(false);
      return;
    }

    GameTime time = ServiceLocator.getTimeSource();
    float deltaTime = time == null ? 0f : time.getDeltaTime();

    if (!Float.isFinite(deltaTime) || deltaTime <= 0f) {
      movement.setMoving(false);
      return;
    }

    if (pauseRemaining > 0f) {
      pauseRemaining = Math.max(0f, pauseRemaining - deltaTime);
      movement.setMoving(false);
      return;
    }

    if (destination == null) {
      chooseNextDestination();
    }

    updateStep(deltaTime);
  }

  @Override
  public void dispose() {
    disposed = true;
    destination = null;
    pauseRemaining = 0f;
  }

  private void chooseNextDestination() {
    if (mode == Mode.FLEE_ALONG_EDGE) {
      destination = chooseEdgeDestination();
    } else {
      destination = choosePlayerDestination();
    }

    float actualDistance = entity.getPosition().dst(destination);

    stepRemaining =
        Math.max(
            MINIMUM_STEP_TIMEOUT, actualDistance / config.bossStepSpeed + MINIMUM_STEP_TIMEOUT);
  }

  private Vector2 choosePlayerDestination() {
    Vector2 direction = target.getCenterPosition().sub(entity.getCenterPosition());
    float availableDistance = Math.max(0f, direction.len() - PLAYER_STOP_DISTANCE);

    float randomDistance = MathUtils.random(config.bossStepMinDistance, config.bossStepMaxDistance);
    float stepDistance = Math.min(randomDistance, availableDistance);

    return clampToVisibleArea(entity.getPosition().mulAdd(direction.nor(), stepDistance));
  }

  private Vector2 chooseEdgeDestination() {
    Rectangle bounds = getVisibleBounds();
    Vector2 position = clampToVisibleArea(entity.getPosition());

    float stepDistance = MathUtils.random(config.bossStepMinDistance, config.bossStepMaxDistance);

    ArrayList<Vector2> candidates = new ArrayList<>();
    float edgeTolerance = ARRIVAL_DISTANCE * 2f;

    boolean nearVerticalEdge =
        Math.abs(position.x - bounds.x) <= edgeTolerance
            || Math.abs(position.x - (bounds.x + bounds.width)) <= edgeTolerance;

    boolean nearHorizontalEdge =
        Math.abs(position.y - bounds.y) <= edgeTolerance
            || Math.abs(position.y - (bounds.y + bounds.height)) <= edgeTolerance;

    if (nearVerticalEdge) {
      // Keep x fixed: move vertically along the left or right edge.
      addEdgeCandidate(
          candidates,
          position,
          new Vector2(
              position.x,
              MathUtils.clamp(position.y + stepDistance, bounds.y, bounds.y + bounds.height)));

      addEdgeCandidate(
          candidates,
          position,
          new Vector2(
              position.x,
              MathUtils.clamp(position.y - stepDistance, bounds.y, bounds.y + bounds.height)));
    }

    if (nearHorizontalEdge) {
      // Keep y fixed: move horizontally along the top or bottom edge.
      addEdgeCandidate(
          candidates,
          position,
          new Vector2(
              MathUtils.clamp(position.x + stepDistance, bounds.x, bounds.x + bounds.width),
              position.y));

      addEdgeCandidate(
          candidates,
          position,
          new Vector2(
              MathUtils.clamp(position.x - stepDistance, bounds.x, bounds.x + bounds.width),
              position.y));
    }

    if (candidates.isEmpty()) {
      return chooseRetreatDestination(position, bounds, stepDistance);
    }

    return chooseFarthestFromPlayer(candidates);
  }

  private void addEdgeCandidate(
      ArrayList<Vector2> candidates, Vector2 position, Vector2 candidate) {
    if (position.dst(candidate) > ARRIVAL_DISTANCE) {
      candidates.add(candidate);
    }
  }

  private Vector2 chooseFarthestFromPlayer(ArrayList<Vector2> candidates) {
    Vector2 playerCentre = target.getCenterPosition();
    Vector2 halfSize = entity.getScale().scl(0.5f);

    Vector2 best = candidates.get(0);
    float bestDistance = -1f;

    for (Vector2 candidate : candidates) {
      float distance = candidate.cpy().add(halfSize).dst2(playerCentre);

      if (distance > bestDistance) {
        bestDistance = distance;
        best = candidate;
      }
    }

    return best.cpy();
  }

  private Vector2 chooseRetreatDestination(Vector2 position, Rectangle bounds, float stepDistance) {
    Vector2 direction = entity.getCenterPosition().sub(target.getCenterPosition());

    if (direction.isZero()) {
      direction.set(1f, 0f);
    }

    direction.nor();

    float horizontalLimit = Float.POSITIVE_INFINITY;
    float verticalLimit = Float.POSITIVE_INFINITY;

    if (direction.x > 0f) {
      horizontalLimit = (bounds.x + bounds.width - position.x) / direction.x;
    } else if (direction.x < 0f) {
      horizontalLimit = (bounds.x - position.x) / direction.x;
    }

    if (direction.y > 0f) {
      verticalLimit = (bounds.y + bounds.height - position.y) / direction.y;
    } else if (direction.y < 0f) {
      verticalLimit = (bounds.y - position.y) / direction.y;
    }

    float distance = Math.max(0f, Math.min(stepDistance, Math.min(horizontalLimit, verticalLimit)));

    return clampToVisibleArea(position.cpy().mulAdd(direction, distance));
  }

  private void updateStep(float deltaTime) {
    // The camera can move or resize after a destination has been selected.
    destination = clampToVisibleArea(destination);
    float distance = entity.getPosition().dst(destination);

    if (distance <= ARRIVAL_DISTANCE) {
      beginPause();
      return;
    }

    stepRemaining -= deltaTime;

    if (stepRemaining <= 0f) {
      // A wall or another entity may prevent arrival.
      beginPause();
      return;
    }

    moveTowards(destination, deltaTime);
  }

  private void beginPause() {
    movement.setMoving(false);
    destination = null;
    pauseRemaining = config.bossStepPauseDuration;
  }

  private void moveTowards(Vector2 position, float deltaTime) {
    Vector2 currentPosition = entity.getPosition();
    Vector2 direction = position.cpy().sub(currentPosition);
    float distance = direction.len();

    if (distance <= ARRIVAL_DISTANCE) {
      movement.setMoving(false);
      return;
    }

    Vector2 forward = direction.nor();
    Vector2 steering =
        mode == Mode.FLEE_ALONG_EDGE ? forward : calculateAvoidanceDirection(forward);
    float stepDistance = Math.min(config.bossStepSpeed * deltaTime, distance);

    Vector2 nextPosition = clampToVisibleArea(currentPosition.cpy().mulAdd(steering, stepDistance));

    Vector2 step = nextPosition.cpy().sub(currentPosition);
    float actualDistance = step.len();

    if (actualDistance <= 0.001f) {
      movement.setMoving(false);
      return;
    }

    float speed = Math.min(config.bossStepSpeed, actualDistance / deltaTime);

    movement.setMaxSpeed(new Vector2(speed, speed));
    movement.setTarget(nextPosition);
    movement.setMoving(true);
  }

  /**
   * Returns towards the visible area if camera movement leaves the Boss outside its safe bounds.
   * Camera recovery takes priority over the normal pause.
   */
  private void keepInsideVisibleArea() {
    Vector2 position = entity.getPosition();
    Vector2 safePosition = clampToVisibleArea(position);

    if (!position.epsilonEquals(safePosition, 0.001f)) {
      movement.setMoving(false);
      entity.setPosition(safePosition);
    }

    if (destination != null) {
      destination = clampToVisibleArea(destination);
    }
  }

  /** Adds lateral steering while retaining forward progress towards the destination. */
  private Vector2 calculateAvoidanceDirection(Vector2 forward) {
    Vector2 separation = calculateSummonSeparation(forward);
    Vector2 sideways = new Vector2(-forward.y, forward.x);

    float lateral = separation.dot(sideways);
    float opposition = separation.dot(forward);

    // A summon directly ahead produces no lateral force, so choose a consistent side.
    if (Math.abs(lateral) < 0.05f && opposition < -0.05f) {
      lateral = Math.min(1f, -opposition);
    }

    float sidewaysStrength = MathUtils.clamp(lateral * 2f, -1.5f, 1.5f);

    return forward.cpy().mulAdd(sideways, sidewaysStrength).nor();
  }

  private Vector2 calculateSummonSeparation(Vector2 forward) {
    Vector2 separation = new Vector2();
    Vector2 bossCentre = entity.getCenterPosition();
    float radius = config.summonAvoidanceRadius;

    if (radius <= 0f) {
      return separation;
    }

    for (Entity summon : activeSummons) {
      FinalBossExplosiveSummonComponent explosive =
          summon.getComponent(FinalBossExplosiveSummonComponent.class);

      if (explosive != null && explosive.hasDetonated()) {
        continue;
      }

      Vector2 away = bossCentre.cpy().sub(summon.getCenterPosition());
      float distance = away.len();

      if (distance >= radius) {
        continue;
      }

      if (distance <= 0.001f) {
        // Pick a sideways direction when the two centres overlap.
        away.set(-forward.y, forward.x);
      } else {
        away.scl(1f / distance);
      }

      float strength = 1f - distance / radius;
      separation.mulAdd(away, strength);
    }

    return separation.limit(1f);
  }

  /** Restricts the entire Boss sprite to the camera rectangle with a small margin. */
  private Rectangle getVisibleBounds() {
    Vector2 centre = target.getCenterPosition();
    float halfWidth = config.bossVisibilityHalfWidth;
    float halfHeight = config.bossVisibilityHalfHeight;

    if (camera != null && camera.viewportWidth > 0f && camera.viewportHeight > 0f) {
      float zoom = 1f;

      if (camera instanceof OrthographicCamera) {
        zoom = ((OrthographicCamera) camera).zoom;
      }

      centre.set(camera.position.x, camera.position.y);
      halfWidth = camera.viewportWidth * zoom * 0.5f;
      halfHeight = camera.viewportHeight * zoom * 0.5f;
    }

    Vector2 halfSize = entity.getScale().scl(0.5f);

    float allowedX = Math.max(0f, halfWidth - halfSize.x - SCREEN_MARGIN);
    float allowedY = Math.max(0f, halfHeight - halfSize.y - SCREEN_MARGIN);

    // Bounds describe the entity's lower-left position, not its centre.
    return new Rectangle(
        centre.x - allowedX - halfSize.x,
        centre.y - allowedY - halfSize.y,
        allowedX * 2f,
        allowedY * 2f);
  }

  private Vector2 clampToVisibleArea(Vector2 position) {
    Rectangle bounds = getVisibleBounds();

    return new Vector2(
        MathUtils.clamp(position.x, bounds.x, bounds.x + bounds.width),
        MathUtils.clamp(position.y, bounds.y, bounds.y + bounds.height));
  }
}
