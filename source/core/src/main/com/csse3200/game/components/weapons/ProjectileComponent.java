package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.raycast.RaycastHit;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Moves a weapon hitbox in a straight line and removes it on its first hit.
 *
 * <p>Stops on enemies and solid obstacles (walls, barrels, rocks). Removal is queued via {@link
 * com.csse3200.game.entities.EntityService#scheduleDisposal(com.csse3200.game.entities.Entity)}
 * because collision events fire while the physics world is locked.
 */
public class ProjectileComponent extends Component {
  /** Enemies and solid walls/rocks. */
  private static final short STOP_LAYERS = PhysicsLayer.NPC | PhysicsLayer.OBSTACLE;

  private final Vector2 velocity;

  /**
   * Last centre position confirmed to be outside every obstacle. Box2D rays never report the
   * fixture they start inside, so each sweep must begin from a point known to be clear or a wall
   * that was already penetrated becomes invisible to it.
   */
  private final Vector2 lastClearCenter = new Vector2();

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

    lastClearCenter.set(entity.getCenterPosition());
  }

  /**
   * Kinematic sensors do not contact static walls, so sweep a ray along the flight path and despawn
   * if a solid obstacle is on it.
   *
   * <p>The sweep runs from the last known-clear position to one physics step beyond the current
   * frame. Physics advances on a fixed timestep, so in a single render frame the body can move up
   * to (deltaTime + one physics step) worth of distance — more than a ray covering only deltaTime.
   * The previous version swept exactly deltaTime ahead of the current centre, which let the body
   * step past the ray's end and into a wall; once inside, later rays started inside the wall's
   * fixture and could never see it, so the arrow escaped out the far side.
   */
  @Override
  public void update() {
    GameTime time = ServiceLocator.getTimeSource();
    if (time == null) {
      return;
    }
    float deltaTime = time.getDeltaTime();
    if (deltaTime <= 0f) {
      return; // Time is paused, so the body is not moving.
    }

    Vector2 center = entity.getCenterPosition();
    float lookahead = deltaTime + PhysicsEngine.PHYSICS_TIMESTEP;
    Vector2 to = center.cpy().mulAdd(velocity, lookahead);
    if (isPathBlocked(lastClearCenter, to)) {
      ServiceLocator.getEntityService().scheduleDisposal(entity);
      return;
    }
    lastClearCenter.set(center);
  }

  /**
   * Whether a solid obstacle lies between two points.
   *
   * @param from start of the path, in world coordinates
   * @param to end of the path, in world coordinates
   * @return true if a wall or other solid obstacle is in the way
   */
  static boolean isPathBlocked(Vector2 from, Vector2 to) {
    return ServiceLocator.getPhysicsService()
        .getPhysics()
        .raycast(from, to, PhysicsLayer.OBSTACLE, new RaycastHit());
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
