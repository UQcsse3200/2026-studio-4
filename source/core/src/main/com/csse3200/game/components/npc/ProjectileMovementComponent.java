package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;

/** Moves a floating demon projectile in one direction. */
public class ProjectileMovementComponent extends Component {
  private final Vector2 direction;
  private final float speed;

  public ProjectileMovementComponent(Vector2 direction, float speed) {
    this.direction = direction.cpy().nor();
    this.speed = speed;
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  public void update(float deltaTime) {
    Vector2 newPosition = entity.getPosition();
    newPosition.mulAdd(direction, speed * deltaTime);
    entity.setPosition(newPosition);
  }
}
