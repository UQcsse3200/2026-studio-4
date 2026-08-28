package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;

/** Each frame: this entity's position = owner.position + offset. */
public class FollowComponent extends Component {
  private final Entity owner;
  private Vector2 localOffset;

  public FollowComponent(Entity owner, Vector2 localOffset) {
    this.owner = owner;
    this.localOffset = localOffset.cpy();
  }

  /** Sword sweeps can change this during the attack. */
  public void setLocalOffset(Vector2 localOffset) {
    this.localOffset = localOffset.cpy();
  }

  @Override
  public void update() {
    // getPosition() is a copy, so add() does not move the owner
    entity.setPosition(owner.getPosition().add(localOffset));
  }
}
