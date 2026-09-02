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
    cameraPosition = entity.getPosition();
  }

  @Override
  public void update() {
    if (camera == null) {
      return;
    }

    if (target!= null) {
     setGoal(target.getCenterPosition());
    }

    //entity.getComponent(WallComponent.class);

    Vector2 velocity = goal.sub(cameraPosition).scl(CAMERA_SPEED);
    cameraPosition.add(velocity);
    camera.getEntity().setPosition(cameraPosition);
  }

  /** Sets the camera that follows this entity. */
  public void setCamera(CameraComponent camera) {
    this.camera = camera;
  }

  public void setTarget(Entity entity) {this.target = entity;}

  public void setGoal(Vector2 goal) {this.goal = goal;}

  public void removeTarget() {this.target = null;}
}
