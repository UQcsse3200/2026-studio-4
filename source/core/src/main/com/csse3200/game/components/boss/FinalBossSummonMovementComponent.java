package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.Collection;

/**
 * Scatters summons, then continuously approaches the player from assigned directions.
 *
 * <p>Each summon retains its angular position around the player. There is no timed fallback to
 * chasing the player's centre.
 */
public class FinalBossSummonMovementComponent extends Component {
  private static final float SCATTER_DISTANCE = 0.5f;
  private static final float SCATTER_TIMEOUT = 0.4f;
  private static final float FORMATION_RADIUS = 2f;
  private static final float CLOSING_SPEED = 1.2f;
  private static final float SLOT_TOLERANCE = 0.4f;
  private static final float ARRIVAL_DISTANCE = 0.05f;
  private static final float ANGLE_TOLERANCE = 12f;
  private static final float ANGULAR_STEP = 20f;
  private static final float SEPARATION_RADIUS = 1f;
  private static final float SEPARATION_WEIGHT = 0.8f;

  private static final float SCREEN_MARGIN = 0.25f;

  private Camera camera;

  private final Entity target;
  private final float movementSpeed;
  private final float approachAngle;
  private final Collection<Entity> activeSummons;

  private PhysicsMovementComponent movement;
  private FinalBossExplosiveSummonComponent explosive;
  private Vector2 scatterTarget;

  private boolean scattering = true;
  private boolean disposed;
  private float scatterElapsed;
  private float formationRadius = FORMATION_RADIUS;
  private float initialFormationRadius = FORMATION_RADIUS;
  private float finalApproachRadius;
  private float arrivalTolerance;

  /** Sets the formation radius before the summon is registered. */
  public void setFormationRadius(float radius) {
    if (!Float.isFinite(radius) || radius <= 0f) {
      throw new IllegalArgumentException("Formation radius must be finite and positive");
    }

    initialFormationRadius = radius;
    formationRadius = radius;
  }

  /**
   * Creates a summon with a persistent approach direction.
   *
   * @param target player being approached
   * @param movementSpeed movement speed for this wave
   * @param approachAngle assigned angle around the player, in degrees
   * @param activeSummons live collection of summons in the current wave
   */
  public FinalBossSummonMovementComponent(
      Entity target, float movementSpeed, float approachAngle, Collection<Entity> activeSummons) {
    if (target == null || activeSummons == null) {
      throw new IllegalArgumentException("Target and active summons must not be null");
    }

    if (!Float.isFinite(movementSpeed) || movementSpeed < 0f || !Float.isFinite(approachAngle)) {
      throw new IllegalArgumentException("Summon movement values are invalid");
    }

    this.target = target;
    this.movementSpeed = movementSpeed;
    this.approachAngle = approachAngle;
    this.activeSummons = activeSummons;
  }

  @Override
  public void create() {
    movement = entity.getComponent(PhysicsMovementComponent.class);
    explosive = entity.getComponent(FinalBossExplosiveSummonComponent.class);

    if (movement == null || explosive == null) {
      throw new IllegalStateException(
          "FinalBossSummonMovementComponent requires movement and explosive components");
    }

    scatterTarget =
        entity.getCenterPosition().add(directionAt(approachAngle).scl(SCATTER_DISTANCE));

    // Aim inside the trigger radius so arrival tolerance does not prevent detonation.
    finalApproachRadius = Math.min(initialFormationRadius, explosive.getTriggerDistance() * 0.75f);
    arrivalTolerance = Math.min(ARRIVAL_DISTANCE, explosive.getTriggerDistance() * 0.1f);
  }

  @Override
  public void update() {
    if (disposed || explosive.hasDetonated()) {
      return;
    }

    keepInsideVisibleArea();

    if (explosive.isWarningActive()) {
      return;
    }

    GameTime time = ServiceLocator.getTimeSource();
    float deltaTime = time == null ? 0f : time.getDeltaTime();

    if (!Float.isFinite(deltaTime) || deltaTime <= 0f) {
      movement.setMoving(false);
      return;
    }

    Vector2 destination = chooseDestination(deltaTime);
    moveTowards(destination, deltaTime);
  }

  @Override
  public void dispose() {
    disposed = true;
  }

  private Vector2 chooseDestination(float deltaTime) {
    if (scattering) {
      scatterElapsed += deltaTime;

      boolean arrived = entity.getCenterPosition().dst(scatterTarget) <= ARRIVAL_DISTANCE;

      if (!arrived && scatterElapsed < SCATTER_TIMEOUT) {
        return scatterTarget.cpy();
      }

      scattering = false;
    }

    return chooseFormationDestination(deltaTime);
  }

  private Vector2 chooseFormationDestination(float deltaTime) {
    Vector2 playerCentre = target.getCenterPosition();
    Vector2 summonCentre = entity.getCenterPosition();

    Vector2 assignedPosition =
        clampCentreToVisibleArea(
            positionAroundPlayer(playerCentre, approachAngle, formationRadius));

    // Near the screen edge, use the direction of the reachable slot.
    Vector2 assignedOffset = assignedPosition.cpy().sub(playerCentre);
    float assignedAngle = assignedOffset.isZero() ? approachAngle : assignedOffset.angleDeg();

    Vector2 relativePosition = summonCentre.cpy().sub(playerCentre);
    float currentAngle = relativePosition.isZero() ? assignedAngle : relativePosition.angleDeg();
    float angleDifference = shortestAngleDifference(currentAngle, assignedAngle);

    if (Math.abs(angleDifference) > ANGLE_TOLERANCE) {
      formationRadius = initialFormationRadius;

      float nextAngle =
          currentAngle + MathUtils.clamp(angleDifference, -ANGULAR_STEP, ANGULAR_STEP);

      // Keep the flanking route compact even when the summon starts far away.
      return clampCentreToVisibleArea(
          positionAroundPlayer(playerCentre, nextAngle, initialFormationRadius));
    }

    if (summonCentre.dst(assignedPosition) <= SLOT_TOLERANCE) {
      formationRadius = Math.max(finalApproachRadius, formationRadius - CLOSING_SPEED * deltaTime);
    }

    return clampCentreToVisibleArea(
        positionAroundPlayer(playerCentre, approachAngle, formationRadius));
  }

  private void moveTowards(Vector2 destination, float deltaTime) {
    Vector2 centre = entity.getCenterPosition();
    Vector2 safeDestination = clampCentreToVisibleArea(destination);
    Vector2 direction = safeDestination.cpy().sub(centre);
    float distance = direction.len();

    if (distance <= arrivalTolerance) {
      movement.setMoving(false);
      return;
    }

    float separationScale =
        scattering ? 1f : MathUtils.clamp(formationRadius / initialFormationRadius, 0f, 1f);

    Vector2 steering =
        direction.nor().mulAdd(calculateSeparation(), SEPARATION_WEIGHT * separationScale).nor();

    float stepDistance = Math.min(movementSpeed * deltaTime, distance);

    // Apply the boundary after separation, so avoidance cannot steer off-screen.
    Vector2 nextCentre = clampCentreToVisibleArea(centre.cpy().mulAdd(steering, stepDistance));

    Vector2 step = nextCentre.sub(centre);
    float actualDistance = step.len();

    if (actualDistance <= 0.001f) {
      movement.setMoving(false);
      return;
    }

    float speed = actualDistance / deltaTime;

    movement.setMaxSpeed(new Vector2(speed, speed));
    movement.setTarget(entity.getPosition().add(step));
    movement.setMoving(true);
  }

  private Vector2 calculateSeparation() {
    Vector2 separation = new Vector2();
    Vector2 centre = entity.getCenterPosition();

    for (Entity other : activeSummons) {
      if (other == entity) {
        continue;
      }

      Vector2 away = centre.cpy().sub(other.getCenterPosition());
      float distance = away.len();

      if (distance >= SEPARATION_RADIUS) {
        continue;
      }

      if (distance <= 0.001f) {
        separation.add(directionAt(approachAngle));
      } else {
        float strength = 1f - distance / SEPARATION_RADIUS;
        separation.mulAdd(away.scl(1f / distance), strength);
      }
    }

    return separation.limit(1f);
  }

  private static Vector2 positionAroundPlayer(Vector2 centre, float angle, float radius) {
    return centre.cpy().add(directionAt(angle).scl(radius));
  }

  private static Vector2 directionAt(float angle) {
    return new Vector2(1f, 0f).setAngleDeg(angle);
  }

  private static float shortestAngleDifference(float from, float to) {
    float difference = (to - from) % 360f;

    if (difference > 180f) {
      difference -= 360f;
    } else if (difference < -180f) {
      difference += 360f;
    }

    return difference;
  }

  /** Uses the same world camera as the Boss. */
  public void setCamera(Camera camera) {
    this.camera = camera;
  }

  /** Constrains an unregistered summon's spawn position to the visible area. */
  public Vector2 clampSpawnPosition(Vector2 position) {
    if (!hasUsableCamera()) {
      return position.cpy();
    }

    Vector2 halfSize = entity.getScale().scl(0.5f);
    Vector2 centre = position.cpy().add(halfSize);

    return clampCentreToVisibleArea(centre).sub(halfSize);
  }

  private boolean hasUsableCamera() {
    return camera != null && camera.viewportWidth > 0f && camera.viewportHeight > 0f;
  }

  private Vector2 clampCentreToVisibleArea(Vector2 centre) {
    if (!hasUsableCamera()) {
      return centre.cpy();
    }

    float zoom = 1f;
    if (camera instanceof OrthographicCamera) {
      zoom = ((OrthographicCamera) camera).zoom;
    }

    Vector2 halfSize = entity.getScale().scl(0.5f);
    float halfWidth = camera.viewportWidth * zoom * 0.5f;
    float halfHeight = camera.viewportHeight * zoom * 0.5f;

    float allowedX = Math.max(0f, halfWidth - halfSize.x - SCREEN_MARGIN);
    float allowedY = Math.max(0f, halfHeight - halfSize.y - SCREEN_MARGIN);

    return new Vector2(
        MathUtils.clamp(centre.x, camera.position.x - allowedX, camera.position.x + allowedX),
        MathUtils.clamp(centre.y, camera.position.y - allowedY, camera.position.y + allowedY));
  }

  /**
   * Corrects displacement caused by camera movement or physics. Called during the normal component
   * update, outside physics collision callbacks.
   */
  private void keepInsideVisibleArea() {
    Vector2 position = entity.getPosition();
    Vector2 safePosition = clampSpawnPosition(position);

    if (!position.epsilonEquals(safePosition, 0.001f)) {
      movement.setMoving(false);
      entity.setPosition(safePosition);
    }
  }
}
