package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.physics.components.PhysicsComponent;

public class ChainRestrictionComponent extends Component {
  private final Vector2 anchorPoint;
  private final float maxRadius;
  private PhysicsComponent physicsComponent;

  public ChainRestrictionComponent(Vector2 anchorPoint, float maxRadius) {
    if (anchorPoint == null || !Float.isFinite(maxRadius) || maxRadius <= 0f) {
      throw new IllegalArgumentException("A valid anchor and positive radius are required");
    }

    this.anchorPoint = anchorPoint.cpy();
    this.maxRadius = maxRadius;
  }

  @Override
  public void create() {
    physicsComponent = entity.getComponent(PhysicsComponent.class);
  }

  @Override
  public void update() {
    Body body = physicsComponent.getBody();
    Vector2 offset = body.getPosition().cpy().sub(anchorPoint);
    float distanceSquared = offset.len2();
    float radiusSquared = maxRadius * maxRadius;

    if (distanceSquared < radiusSquared) {
      return;
    }
    Vector2 outward = offset.nor();

    if (distanceSquared > radiusSquared) {
      Vector2 boundaryPosition = anchorPoint.cpy().mulAdd(outward, maxRadius);
      entity.setPosition(boundaryPosition);
    }
    Vector2 velocity = body.getLinearVelocity().cpy();
    float outwardSpeed = velocity.dot(outward);

    if (outwardSpeed > 0f) {
      velocity.mulAdd(outward, -outwardSpeed);
      body.setLinearVelocity(velocity);
    }
  }
}
