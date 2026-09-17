package com.csse3200.game.components.items;

import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.RotatingTextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Continuously rotates a dropped item's visual until the item is picked up or disposed. */
public class ItemSpinComponent extends Component {
  private static final float DEFAULT_ROTATION_SPEED = 60f;

  private final float rotationSpeed;
  private RotatingTextureRenderComponent renderer;
  private float rotation;

  public ItemSpinComponent() {
    this(DEFAULT_ROTATION_SPEED);
  }

  public ItemSpinComponent(float rotationSpeed) {
    this.rotationSpeed = rotationSpeed;
  }

  @Override
  public void create() {
    renderer = entity.getComponent(RotatingTextureRenderComponent.class);
  }

  @Override
  public void update() {
    if (renderer == null) {
      return;
    }

    var timeSource = ServiceLocator.getTimeSource();
    if (timeSource == null) {
      return;
    }

    float deltaTime = timeSource.getDeltaTime();
    if (!Float.isFinite(deltaTime) || deltaTime <= 0f) {
      return;
    }

    rotation = (rotation + rotationSpeed * deltaTime) % 360f;
    renderer.setRotation(rotation);
  }
}
