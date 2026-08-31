package com.csse3200.game.components;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.components.HitboxComponent;

/**
 * Triggers an explosion animation when the entity collides with the player.
 *
 * <p>This component listens for the entity's collisionStart event and checks whether the collision
 * involves this entity's hitbox and the configured player entity. When a valid collision occurs,
 * the component triggers the dieAnimation event.
 */
public class ExplodeComponent extends Component {

  /** The player entity that triggers the explosion upon collision. */
  private final Entity player;

  /** The hitbox component used to identify this entity's collision fixture. */
  private HitboxComponent hitboxComponent;

  /**
   * Creates an ExplodeComponent that explodes when it collides with the specified player.
   *
   * @param player the player entity that triggers the explosion
   */
  public ExplodeComponent(Entity player) {
    this.player = player;
  }

  /** Registers a listener for collision events and retrieves this entity's hitbox component. */
  @Override
  public void create() {
    super.create();

    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    hitboxComponent = entity.getComponent(HitboxComponent.class);
  }

  /**
   * Handles the start of a collision involving this entity.
   *
   * <p>Collisions involving a different fixture are ignored. The other fixture's body user data is
   * then checked to determine whether it belongs to the configured player entity.
   *
   * @param me the fixture belonging to this entity
   * @param other the fixture belonging to the other colliding entity
   */
  private void onCollisionStart(Fixture me, Fixture other) {
    // Ignore collisions involving a different fixture.
    if (hitboxComponent.getFixture() != me) {
      return;
    }

    // Get the entity associated with the other fixture.
    if (!(other.getBody().getUserData() instanceof BodyUserData)) {
      return;
    }

    Entity collidedEntity = ((BodyUserData) other.getBody().getUserData()).entity;

    if (collidedEntity == player) {
      explode();
    }
  }

  /** Triggers the entity's death animation event. */
  private void explode() {
    entity.getEvents().trigger("dieAnimation");
  }
}
