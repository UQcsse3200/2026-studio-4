package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.physics.components.PhysicsComponent;

public class ChainRestrictionComponent extends Component {
  private final Vector2 anchorPoint;
  private final float maxRadius;
  private PhysicsComponent physicsComponent;

  public ChainRestrictionComponent(Vector2 anchorPoint, float maxRadius) {
    this.anchorPoint = anchorPoint;
    this.maxRadius = maxRadius;
  }

  @Override
  public void create() {
    super.create();
    physicsComponent = entity.getComponent(PhysicsComponent.class);
  }

  @Override
  public void update() {
    Vector2 currentPoint = entity.getPosition();
    float distance = currentPoint.dst(anchorPoint);
    if (distance > maxRadius) {
      Vector2 pullDirection = anchorPoint.cpy().sub(currentPoint).nor();
      physicsComponent
          .getBody()
          .applyLinearImpulse(
              pullDirection.scl(20f), physicsComponent.getBody().getWorldCenter(), true);
    }
  }
}
