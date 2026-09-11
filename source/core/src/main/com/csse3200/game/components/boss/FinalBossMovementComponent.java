package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/** Moves Grandpa towards the player in discrete steps separated by pauses. */
public class FinalBossMovementComponent extends Component {
  /** Available Stage 1 movement modes. */
  public enum Mode {
    STOPPED,
    STEP_TOWARDS_PLAYER
  }

  private static final float ARRIVAL_DISTANCE = 0.1f;
  private static final float SCREEN_MARGIN = 0.25f;
  private static final float PLAYER_STOP_DISTANCE = 0.75f;
  private static final float MINIMUM_STEP_TIMEOUT = 2f;

  private final Entity target;
  private final FinalBossStageOneConfig config;

  private PhysicsMovementComponent movement;
  private Camera camera;
  private Mode mode = Mode.STOPPED;
  private Vector2 destination;

  private float pauseRemaining;
  private float stepRemaining;
  private boolean disposed;

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

  /** Changes behaviour and resets any previous movement or pause. */
  public void setMode(Mode mode) {
    if (mode == null) {
      throw new IllegalArgumentException("Movement mode must not be null");
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

    if (returnToVisibleArea(deltaTime)) {
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
    Vector2 direction = target.getCenterPosition().sub(entity.getCenterPosition());
    float availableDistance = Math.max(0f, direction.len() - PLAYER_STOP_DISTANCE);

    float randomDistance = MathUtils.random(config.bossStepMinDistance, config.bossStepMaxDistance);
    float stepDistance = Math.min(randomDistance, availableDistance);

    destination = clampToVisibleArea(entity.getPosition().mulAdd(direction.nor(), stepDistance));

    float actualDistance = entity.getPosition().dst(destination);
    stepRemaining =
        Math.max(
            MINIMUM_STEP_TIMEOUT, actualDistance / config.bossStepSpeed + MINIMUM_STEP_TIMEOUT);
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
    float distance = entity.getPosition().dst(position);

    // Slow down for the final step to reduce overshooting at high movement speeds.
    float speed = Math.min(config.bossStepSpeed, distance / deltaTime);

    movement.setMaxSpeed(new Vector2(speed, speed));
    movement.setTarget(position.cpy());
    movement.setMoving(true);
  }

  /**
   * Returns towards the visible area if camera movement leaves the Boss outside its safe bounds.
   * Camera recovery takes priority over the normal pause.
   */
  private boolean returnToVisibleArea(float deltaTime) {
    Vector2 position = entity.getPosition();
    Vector2 safePosition = clampToVisibleArea(position);

    if (position.epsilonEquals(safePosition, ARRIVAL_DISTANCE)) {
      return false;
    }

    destination = null;
    pauseRemaining = 0f;
    moveTowards(safePosition, deltaTime);
    return true;
  }

  /** Restricts the entire Boss sprite to the camera rectangle with a small margin. */
  private Vector2 clampToVisibleArea(Vector2 position) {
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
    Vector2 desiredCentre = position.cpy().add(halfSize);

    float allowedX = Math.max(0f, halfWidth - halfSize.x - SCREEN_MARGIN);
    float allowedY = Math.max(0f, halfHeight - halfSize.y - SCREEN_MARGIN);

    desiredCentre.x = MathUtils.clamp(desiredCentre.x, centre.x - allowedX, centre.x + allowedX);
    desiredCentre.y = MathUtils.clamp(desiredCentre.y, centre.y - allowedY, centre.y + allowedY);

    return desiredCentre.sub(halfSize);
  }
}
