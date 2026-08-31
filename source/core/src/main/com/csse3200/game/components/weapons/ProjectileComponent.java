package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.raycast.RaycastHit;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Moves a weapon hitbox in a straight line and removes it on its first hit.
 *
 * <p>Stops on enemies and solid obstacles (walls, rocks). Removal is queued via {@link
 * com.csse3200.game.entities.EntityService#scheduleDisposal(com.csse3200.game.entities.Entity)}
 * because collision events fire while the physics world is locked.
 */
public class ProjectileComponent extends Component {
  /** Enemies and solid walls/rocks. */
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

    // Stay kinematic so debug outlines match sword/knife (Box2D draws kinematic sensors blue).
    // Damping is zeroed so the projectile keeps a constant speed (the default slows it down).
    Body body = entity.getComponent(PhysicsComponent.class).getBody();
    body.setLinearDamping(0f);
    body.setLinearVelocity(velocity);
  }

  /**
   * Kinematic sensors do not contact static walls, so raycast the next step for solid obstacles
   * ({@link PhysicsLayer#OBSTACLE}).
   */
  @Override
  public void update() {
    GameTime time = ServiceLocator.getTimeSource();
    if (time == null) {
      return;
    }
    Vector2 from = entity.getCenterPosition();
    Vector2 to = from.cpy().mulAdd(velocity, time.getDeltaTime());
    if (from.epsilonEquals(to)) {
      return;
    }
    RaycastHit hit = new RaycastHit();
    if (ServiceLocator.getPhysicsService()
        .getPhysics()
        .raycast(from, to, PhysicsLayer.OBSTACLE, hit)) {
      ServiceLocator.getEntityService().scheduleDisposal(entity);
    }
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      return;
    }
    // Ignore anything that is not an enemy or a solid obstacle.
    if (!PhysicsLayer.contains(STOP_LAYERS, other.getFilterData().categoryBits)) {
      return;
    }
    ServiceLocator.getEntityService().scheduleDisposal(entity);
  }
}
