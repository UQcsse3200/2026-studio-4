package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;

/** Moves a camera smoothly toward the entity this component is attached to. */
public class FollowingCameraComponent extends Component {
  private static final float CAMERA_SPEED = 0.1f;

  private CameraComponent camera;
  private Vector2 cameraPosition;
  private Entity target;
  private Vector2 goal;

  @Override
  public void create() {
    cameraPosition = entity.getCenterPosition();
  }

  @Override
  public void update() {
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
