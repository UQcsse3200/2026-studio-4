package com.csse3200.game.components.weapons;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.raycast.RaycastHit;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;

/**
 * Moves a weapon hitbox in a straight line and removes it on its first hit.
 *
 * <p>Stops on enemies and solid obstacles (walls, rocks). Holes are pits, not walls, so arrows fly
 * over them; they are recognised by their texture until they get a physics layer of their own.
 * Removal is queued via {@link
 * com.csse3200.game.entities.EntityService#scheduleDisposal(com.csse3200.game.entities.Entity)}
 * because collision events fire while the physics world is locked.
 */
public class ProjectileComponent extends Component {
  /** Enemies and solid walls/rocks. */
  private static final short STOP_LAYERS = PhysicsLayer.NPC | PhysicsLayer.OBSTACLE;

  /** Holes share the obstacle layer, so they are identified by the texture they render. */
  private static final String HOLE_TEXTURE = "images/hole.png";

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
   * Kinematic sensors do not contact static walls, so raycast the next movement step and despawn if
   * a solid obstacle is ahead.
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
    if (isPathBlocked(from, to)) {
      ServiceLocator.getEntityService().scheduleDisposal(entity);
    }
  }

  /**
   * Whether a solid obstacle lies between two points. Holes do not count: arrows fly over them.
   *
   * @param from start of the path, in world coordinates
   * @param to end of the path, in world coordinates
   * @return true if a wall or other solid obstacle is in the way
   */
  static boolean isPathBlocked(Vector2 from, Vector2 to) {
    RaycastHit[] hits =
        ServiceLocator.getPhysicsService().getPhysics().raycastAll(from, to, PhysicsLayer.OBSTACLE);
    for (RaycastHit hit : hits) {
      if (!isHole(hit.fixture)) {
        return true;
      }
    }
    return false;
  }

  /** True if the fixture belongs to an entity rendering the hole texture. */
  private static boolean isHole(Fixture fixture) {
    Object userData = fixture.getBody().getUserData();
    if (!(userData instanceof BodyUserData) || ((BodyUserData) userData).entity == null) {
      return false;
    }
    TextureRenderComponent render =
        ((BodyUserData) userData).entity.getComponent(TextureRenderComponent.class);
    ResourceService resources = ServiceLocator.getResourceService();
    return render != null
        && resources != null
        && resources.containsAsset(HOLE_TEXTURE, Texture.class)
        && render.getTexture() == resources.getAsset(HOLE_TEXTURE, Texture.class);
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
