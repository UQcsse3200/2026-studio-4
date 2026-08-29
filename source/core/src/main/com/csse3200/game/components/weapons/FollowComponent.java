package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;

/**
 * Each frame, moves this entity to {@code owner.position + localOffset} so a weapon sensor travels
 * with a moving wielder.
 *
 * <p>Offset is in world axes and is locked at attack start unless {@link #setLocalOffset(Vector2)}
 * is called (used later for a sword sweep).
 */
public class FollowComponent extends Component {
  private final Entity owner;
  private Vector2 localOffset;

  /**
   * @param owner entity to track; typically the wielder
   * @param localOffset world-axis offset from the owner's position
   * @require owner != null &amp;&amp; localOffset != null
   * @throws IllegalArgumentException if owner or localOffset is null
   */
  public FollowComponent(Entity owner, Vector2 localOffset) {
    if (owner == null || localOffset == null) {
      throw new IllegalArgumentException("owner and localOffset must not be null");
    }
    this.owner = owner;
    this.localOffset = localOffset.cpy();
  }

  /**
   * Replace the follow offset. Sword sweeps can mutate this over the attack lifetime.
   *
   * @param localOffset new world-axis offset from the owner
   * @require localOffset != null
   * @throws IllegalArgumentException if localOffset is null
   */
  public void setLocalOffset(Vector2 localOffset) {
    if (localOffset == null) {
      throw new IllegalArgumentException("localOffset must not be null");
    }
    this.localOffset = localOffset.cpy();
  }

  /**
   * @return copy of the current offset
   */
  public Vector2 getLocalOffset() {
    return localOffset.cpy();
  }

  /**
   * @return the tracked owner
   */
  public Entity getOwner() {
    return owner;
  }

  @Override
  public void update() {
    entity.setPosition(owner.getPosition().add(localOffset));
  }
}
