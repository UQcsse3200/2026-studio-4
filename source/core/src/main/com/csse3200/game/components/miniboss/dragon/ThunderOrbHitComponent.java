package com.csse3200.game.components.miniboss.dragon;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Handles single-hit damage, impact playback, and safe removal of a thunder orb. */
public class ThunderOrbHitComponent extends Component {
  public static final String IMPACT_ANIMATION = "impact";

  private static final float IMPACT_DURATION = 6 * 0.085f;

  private final int damage;
  private HitboxComponent hitbox;
  private ThunderOrbMovementComponent movement;
  private AnimationRenderComponent animator;

  private boolean impacted;
  private boolean removalQueued;
  private float impactRemaining;

  public ThunderOrbHitComponent(int damage) {
    if (damage <= 0) {
      throw new IllegalArgumentException("Damage must be positive");
    }
    this.damage = damage;
  }

  @Override
  public void create() {
    hitbox = entity.getComponent(HitboxComponent.class);
    movement = entity.getComponent(ThunderOrbMovementComponent.class);
    animator = entity.getComponent(AnimationRenderComponent.class);

    if (hitbox == null || movement == null) {
      throw new IllegalStateException(
          "ThunderOrbHitComponent requires hitbox and movement components");
    }

    entity.getEvents().addListener("collisionStart", this::onCollision);
    entity.getEvents().addListener(ThunderOrbMovementComponent.EXPIRED, this::onExpired);
  }

  private void onCollision(Fixture me, Fixture other) {
    if (impacted || removalQueued || me != hitbox.getFixture()) {
      return;
    }
    if (!PhysicsLayer.contains(PhysicsLayer.PLAYER, other.getFilterData().categoryBits)) {
      return;
    }

    Object userData = other.getBody().getUserData();
    if (!(userData instanceof BodyUserData data) || data.entity == null) {
      return;
    }

    Entity target = data.entity;
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    if (stats == null || stats.isDead() || StatusEffectsControllerComponent.isConcealed(target)) {
      return;
    }
    impacted = true;
    movement.stop();
    stats.takeDamage(damage, entity);
    playImpactIfActive();
  }

  private void playImpactIfActive() {
    if (removalQueued) {
      return;
    }

    if (animator != null && animator.hasAnimation(IMPACT_ANIMATION)) {
      impactRemaining = IMPACT_DURATION;
      animator.startAnimation(IMPACT_ANIMATION);
    } else {
      cancel();
    }
  }

  private void onExpired() {
    if (!impacted) {
      cancel();
    }
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  /** Advances the impact effect without allowing further damage. */
  public void update(float delta) {
    if (!impacted || removalQueued || !Float.isFinite(delta) || delta <= 0f) {
      return;
    }

    impactRemaining -= delta;
    if (impactRemaining <= 0f) {
      cancel();
    }
  }

  /** Stops and queues removal, including when the owning dragon dies. */
  public void cancel() {
    if (removalQueued) {
      return;
    }

    removalQueued = true;
    movement.stop();
    ServiceLocator.getEntityService().scheduleDisposal(entity);
  }
}
