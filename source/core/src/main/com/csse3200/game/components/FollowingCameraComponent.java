package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;

public class FollowingCameraComponent extends Component {
  private static float CAMERA_SPEED = 0.1f;
  private CameraComponent camera;
  private Vector2 cameraPosition;

  @Override
  public void create() {
    cameraPosition = entity.getPosition();
  }

  @Override
  public void update() {
    if (camera == null) return;

    Vector2 velocity = entity.getPosition().sub(cameraPosition).scl(CAMERA_SPEED);
    cameraPosition.add(velocity);
    camera.getEntity().setPosition(cameraPosition);
  }

  public void setCamera(CameraComponent camera) {
    this.camera = camera;
  }
}
