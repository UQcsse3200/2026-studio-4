package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Moves a camera smoothly toward the entity this component is attached to. */
public class FollowingCameraComponent extends Component {
  private static final float CAMERA_SPEED = 0.1f;

  private CameraComponent camera;
  private Vector2 cameraPosition;
  private Entity target;
  private Vector2 goal;
  private final Set<Object> positionLocks = Collections.newSetFromMap(new IdentityHashMap<>());
  private Vector2 lockedPosition;

  @Override
  public void create() {
    cameraPosition = entity.getCenterPosition();
  }

  @Override
  public void update() {
    if (camera != null && !positionLocks.isEmpty()) {
      applyLockedPosition();
      return;
    }
    if (camera != null && target != null) {
      this.setGoal(target.getCenterPosition());
      Vector2 maxWallBounds = entity.getComponent(WallComponent.class).getWallBounds();
      Vector2 minWallBounds = entity.getCenterPosition();
      Vector2 velocity = goal.sub(cameraPosition).scl(CAMERA_SPEED);
      Vector2 futureLocation = new Vector2(cameraPosition.x, cameraPosition.y).add(velocity);
      Vector2 cameraSize = camera.getCameraSize();
      cameraSize.scl(0.5f);

      if ((maxWallBounds.x < (futureLocation.x + cameraSize.x))) {
        futureLocation.set(maxWallBounds.x - cameraSize.x, futureLocation.y);
      }
      if ((minWallBounds.x > (futureLocation.x - cameraSize.x))) {
        futureLocation.set(minWallBounds.x + cameraSize.x, futureLocation.y);
      }
      if ((maxWallBounds.x / 2) < cameraSize.x) {
        futureLocation.set(target.getCenterPosition());
      }
      if ((maxWallBounds.y < (futureLocation.y + cameraSize.y))) {
        futureLocation.set(futureLocation.x, maxWallBounds.y - cameraSize.y);
      }
      if ((minWallBounds.y > (futureLocation.y - cameraSize.y))) {
        futureLocation.set(futureLocation.x, minWallBounds.y + cameraSize.y);
      }
      cameraPosition = futureLocation;
      camera.getEntity().setPosition(cameraPosition);
    }
  }

  /**
   * Holds the current view without forgetting the followed target. Repeated requests from the same
   * owner are idempotent; other owners must release their own requests before following resumes.
   *
   * @return whether a camera was available to lock
   */
  public boolean lockPosition(Object owner) {
    if (owner == null) throw new IllegalArgumentException("Camera lock owner must not be null");
    if (camera == null) return false;
    if (positionLocks.isEmpty()) {
      lockedPosition = new Vector2(camera.getCamera().position.x, camera.getCamera().position.y);
    }
    positionLocks.add(owner);
    applyLockedPosition();
    return true;
  }

  /** Releases only this owner's request and resumes smoothly from the last fixed position. */
  public void unlockPosition(Object owner) {
    if (positionLocks.remove(owner) && positionLocks.isEmpty()) {
      cameraPosition = lockedPosition.cpy();
      lockedPosition = null;
    }
  }

  private void applyLockedPosition() {
    cameraPosition = lockedPosition.cpy();
    camera.getEntity().setPosition(cameraPosition);
    camera.getCamera().position.set(cameraPosition.x, cameraPosition.y, 0f);
    camera.getCamera().update();
  }

  /** Sets the camera that follows this entity. */
  public void setCamera(CameraComponent camera) {
    this.camera = camera;
  }

  public void setTarget(Entity entity) {
    this.target = entity;
  }

  public void setGoal(Vector2 goal) {
    this.goal = goal;
  }

  public void removeTarget() {
    this.target = null;
  }
}
