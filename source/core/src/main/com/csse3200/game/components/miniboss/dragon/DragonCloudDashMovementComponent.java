package com.csse3200.game.components.miniboss.dragon;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.ServiceLocator;

/** Moves a cloud dash along its locked direction while respecting obstacles. */
public class DragonCloudDashMovementComponent extends Component {

  public static final String DASH_STEP = "dragonCloudDashStep";
  private static final float SPEED = 6f;
  private static final float MAX_DISTANCE = 3f;
  private static final float MAX_STEP = 0.05f;
  private static final float EPSILON = 0.0001f;

  private DragonCloudDashComponent dragonCloudDash;
  private CombatStatsComponent combatStats;
  private PhysicsMovementComponent normalMovement;
  private ColliderComponent collider;
  private Body body;
  private World world;
  private float travelled;

  @Override
  public void create() {
    dragonCloudDash = entity.getComponent(DragonCloudDashComponent.class);
    combatStats = entity.getComponent(CombatStatsComponent.class);
    collider = entity.getComponent(ColliderComponent.class);
    normalMovement = entity.getComponent(PhysicsMovementComponent.class);
    PhysicsComponent physics = entity.getComponent(PhysicsComponent.class);

    if (dragonCloudDash == null || combatStats == null || collider == null || physics == null) {
      throw new IllegalStateException(
          "Cloud dash movement requires dash, stats, collider, and physics");
    }

    body = physics.getBody();
    world = body.getWorld();
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  /** Advances dash movement without exceeding the remaining distance. */
  public void update(float delta) {
    DragonCloudDashComponent.State state = dragonCloudDash.getState();

    if (combatStats.isDead()) {
      dragonCloudDash.stop();
      stopMovement();
      return;
    }

    if (StatusEffectsControllerComponent.isImmobilised(entity)) {
      stopMovement();
      return;
    }

    if (state == DragonCloudDashComponent.State.READY) {
      travelled = 0f;
      return;
    }

    stopMovement();

    if (state != DragonCloudDashComponent.State.DASHING) {
      travelled = 0f;
      return;
    }

    if (!Float.isFinite(delta) || delta <= 0f) {
      return;
    }

    moveDash(delta);
  }

  private void stopMovement() {
    if (normalMovement != null) {
      normalMovement.setMoving(false);
    }
    body.setLinearVelocity(0f, 0f);
  }

  private void moveDash(float delta) {
    float distance = Math.min(SPEED * delta, MAX_DISTANCE - travelled);
    Vector2 direction = dragonCloudDash.getLockedDirection();

    while (distance > EPSILON) {
      float step = Math.min(MAX_STEP, distance);
      Vector2 displacement = direction.cpy().scl(step);

      if (wouldHitObstacle(displacement)) {
        dragonCloudDash.finishDash();
        return;
      }

      Vector2 from = entity.getPosition().cpy();
      entity.setPosition(entity.getPosition().add(displacement));
      travelled += step;
      distance -= step;

      entity.getEvents().trigger(DASH_STEP, from, entity.getPosition().cpy());
    }

    if (MAX_DISTANCE - travelled <= EPSILON) {
      dragonCloudDash.finishDash();
    }
  }

  /**
   * Checks the swept bounding box of the body's polygon collider. This is conservative around
   * corners: stopping early is preferable to clipping.
   */
  private boolean wouldHitObstacle(Vector2 displacement) {
    if (collider.getFixture() == null
        || !(collider.getFixture().getShape() instanceof PolygonShape polygon)) {
      throw new IllegalStateException("Cloud dash requires a created polygon collider");
    }

    Vector2 vertex = new Vector2();
    float minX = Float.POSITIVE_INFINITY;
    float minY = Float.POSITIVE_INFINITY;
    float maxX = Float.NEGATIVE_INFINITY;
    float maxY = Float.NEGATIVE_INFINITY;

    for (int i = 0; i < polygon.getVertexCount(); i++) {
      polygon.getVertex(i, vertex);
      Vector2 point = body.getWorldPoint(vertex);
      minX = Math.min(minX, point.x);
      minY = Math.min(minY, point.y);
      maxX = Math.max(maxX, point.x);
      maxY = Math.max(maxY, point.y);
    }

    boolean[] blocked = {false};
    world.QueryAABB(
        fixture -> {
          if (fixture.getBody() != body
              && !fixture.isSensor()
              && PhysicsLayer.contains(
                  fixture.getFilterData().categoryBits, PhysicsLayer.OBSTACLE)) {
            blocked[0] = true;
            return false;
          }
          return true;
        },
        minX + Math.min(0f, displacement.x),
        minY + Math.min(0f, displacement.y),
        maxX + Math.max(0f, displacement.x),
        maxY + Math.max(0f, displacement.y));

    return blocked[0];
  }
}
