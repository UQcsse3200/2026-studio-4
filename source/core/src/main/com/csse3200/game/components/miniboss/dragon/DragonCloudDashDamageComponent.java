package com.csse3200.game.components.miniboss.dragon;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;

/** Applies at most one damage attempt to the target per cloud dash. */
public class DragonCloudDashDamageComponent extends Component {
  private static final int DAMAGE = 20;

  private final Entity target;

  private DragonCloudDashComponent dragonCloudDash;
  private CombatStatsComponent ownerStats;
  private CombatStatsComponent targetStats;
  private ColliderComponent ownerCollider;
  private HitboxComponent targetHitbox;
  private boolean hit;
  private boolean disposed;

  public DragonCloudDashDamageComponent(Entity target) {
    if (target == null) {
      throw new IllegalArgumentException("Target is required");
    }
    this.target = target;
  }

  @Override
  public void create() {
    dragonCloudDash = entity.getComponent(DragonCloudDashComponent.class);
    ownerStats = entity.getComponent(CombatStatsComponent.class);
    targetStats = target.getComponent(CombatStatsComponent.class);
    ownerCollider = entity.getComponent(ColliderComponent.class);
    targetHitbox = target.getComponent(HitboxComponent.class);

    if (dragonCloudDash == null
        || ownerStats == null
        || targetStats == null
        || ownerCollider == null
        || targetHitbox == null) {
      throw new IllegalStateException(
          "Dash damage requires dash, combat stats, owner collider, and target hitbox");
    }

    entity.getEvents().addListener(DragonCloudDashComponent.ATTACK_STARTED, this::resetHit);
    entity.getEvents().addListener(DragonCloudDashMovementComponent.DASH_STEP, this::onDashStep);
  }

  private void resetHit() {
    hit = false;
  }

  private void onDashStep(Vector2 from, Vector2 to) {
    if (disposed
        || hit
        || dragonCloudDash.getState() != DragonCloudDashComponent.State.DASHING
        || ownerStats.isDead()
        || targetStats.isDead()
        || StatusEffectsControllerComponent.isImmobilised(entity)
        || StatusEffectsControllerComponent.isConcealed(target)) {
      return;
    }

    if (!crossesTarget(from, to)) {
      return;
    }

    hit = true;
    targetStats.takeDamage(DAMAGE, entity);
  }

  private boolean crossesTarget(Vector2 from, Vector2 to) {
    Rectangle ownerBounds = getBounds(ownerCollider.getFixture());
    Rectangle targetBounds = getBounds(targetHitbox.getFixture());

    Vector2 end = new Vector2(ownerBounds.x, ownerBounds.y);
    Vector2 start = end.cpy().sub(to.cpy().sub(from));

    Rectangle expandedTarget =
        new Rectangle(
            targetBounds.x - ownerBounds.width,
            targetBounds.y - ownerBounds.height,
            targetBounds.width + ownerBounds.width,
            targetBounds.height + ownerBounds.height);

    return expandedTarget.contains(start)
        || expandedTarget.contains(end)
        || Intersector.intersectSegmentRectangle(start, end, expandedTarget);
  }

  private Rectangle getBounds(Fixture fixture) {
    if (fixture == null || !(fixture.getShape() instanceof PolygonShape polygon)) {
      throw new IllegalStateException("Dash damage requires created polygon fixtures");
    }

    Vector2 vertex = new Vector2();
    float minX = Float.POSITIVE_INFINITY;
    float minY = Float.POSITIVE_INFINITY;
    float maxX = Float.NEGATIVE_INFINITY;
    float maxY = Float.NEGATIVE_INFINITY;

    for (int i = 0; i < polygon.getVertexCount(); i++) {
      polygon.getVertex(i, vertex);
      Vector2 point = fixture.getBody().getWorldPoint(vertex);
      minX = Math.min(minX, point.x);
      minY = Math.min(minY, point.y);
      maxX = Math.max(maxX, point.x);
      maxY = Math.max(maxY, point.y);
    }

    return new Rectangle(minX, minY, maxX - minX, maxY - minY);
  }

  @Override
  public void dispose() {
    disposed = true;
    super.dispose();
  }
}
