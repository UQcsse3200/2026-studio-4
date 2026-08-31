package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Moves a weapon hitbox in a straight line and removes it on its first hit.
 *
 * <p>The hitbox despawns when it touches an enemy (damage is applied by {@code
 * TouchAttackComponent} on the same entity) or an obstacle such as a wall or rock. Removal is
 * queued via {@link
 * com.csse3200.game.entities.EntityService#scheduleDisposal(com.csse3200.game.entities.Entity)}
 * because collision events fire while the physics world is locked.
 */
public class ProjectileComponent extends Component {
  /** Layers that stop the projectile: enemies it can damage, and impassable obstacles. */
  private static final short STOP_LAYERS = PhysicsLayer.NPC | PhysicsLayer.OBSTACLE;

  private final Vector2 velocity;
  private HitboxComponent hitboxComponent;

  /**
   * @param direction travel direction; does not need to be normalised
   * @param speed travel speed in metres per second
   * @throws IllegalArgumentException if direction is null or zero, or speed is not positive
   */
  public ProjectileComponent(Vector2 direction, float speed) {
    if (direction == null || direction.isZero() || speed <= 0f) {
      throw new IllegalArgumentException("direction must be non-zero and speed positive");
    }
    this.velocity = direction.cpy().nor().scl(speed);
  }

  @Override
  public void create() {
    hitboxComponent = entity.getComponent(HitboxComponent.class);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);

    // A kinematic body gets no contacts against static walls and rocks, so use a dynamic body.
    // Damping is zeroed so the projectile keeps a constant speed (the default slows it down).
    Body body = entity.getComponent(PhysicsComponent.class).getBody();
    body.setType(BodyType.DynamicBody);
    body.setLinearDamping(0f);
    body.setLinearVelocity(velocity);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      return;
    }
    // Ignore the wielder and other non-blocking overlaps at spawn.
    if (!PhysicsLayer.contains(STOP_LAYERS, other.getFilterData().categoryBits)) {
      return;
    }
    ServiceLocator.getEntityService().scheduleDisposal(entity);
  }
}
