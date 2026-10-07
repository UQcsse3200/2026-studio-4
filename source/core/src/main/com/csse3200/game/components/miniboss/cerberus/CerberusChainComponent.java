package com.csse3200.game.components.miniboss.cerberus;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;

public class CerberusChainComponent extends Component {
  private final Vector2 wallAnchor;

  public CerberusChainComponent(Vector2 wallAnchor) {
    if (wallAnchor == null || !Float.isFinite(wallAnchor.x) || !Float.isFinite(wallAnchor.y)) {
      throw new IllegalArgumentException("A finite wall anchor is required");
    }
    this.wallAnchor = wallAnchor.cpy();
  }

  public Vector2 getWallAnchor() {
    return wallAnchor.cpy();
  }

  /** Returns a point on the torso, independent of whether the middle head lives. */
  public Vector2 getBodyAttachment() {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();

    return new Vector2(position.x + scale.x * 0.5f, position.y + scale.y * 0.4f);
  }

  public float getEndpointDistance() {
    return wallAnchor.dst(getBodyAttachment());
  }
}
