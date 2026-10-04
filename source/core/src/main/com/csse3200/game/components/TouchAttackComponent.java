package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * When this entity touches a valid enemy's hitbox, deal damage to them and apply a knockback.
 *
 * <p>Requires CombatStatsComponent, HitboxComponent on this entity.
 *
 * <p>Damage is only applied if target entity has a CombatStatsComponent. Knockback is only applied
 * if target entity has a PhysicsComponent.
 *
 * <p>Normally the knockback pushes the target away after a hit, so later damage comes from a fresh
 * collision once they touch again. If the target can't actually be pushed away - for example, it's
 * immobilised by a status effect such as Frozen, and something zeroes its velocity every frame -
 * the two stay in continuous contact without a new collision ever firing, so the target would
 * otherwise sit right next to the attacker taking no further damage. While contact is sustained,
 * this component re-applies the attack on a fixed interval so stuck-together contact still deals
 * damage over time instead of going silent.
 */
public class TouchAttackComponent extends Component {
  /** How often (in ms) a sustained, unbroken contact re-deals damage. */
  private static final long REPEAT_HIT_INTERVAL_MS = 1000;

  private short targetLayer;
  private float knockbackForce = 0f;
  private CombatStatsComponent combatStats;
  private HitboxComponent hitboxComponent;

  /** The entity currently overlapping our hitbox, or null if contact has ended. */
  private Entity overlappingTarget;

  private long lastHitTime;

  /**
   * Create a component which attacks entities on collision, without knockback.
   *
   * @param targetLayer The physics layer of the target's collider.
   */
  public TouchAttackComponent(short targetLayer) {
    this.targetLayer = targetLayer;
  }

  /** Returns the victim layer mask, including PLAYER for hostile projectiles. */
  public short getTargetLayer() {
    return targetLayer;
  }

  /**
   * Create a component which attacks entities on collision, with knockback.
   *
   * @param targetLayer The physics layer of the target's collider.
   * @param knockback The magnitude of the knockback applied to the entity.
   */
  public TouchAttackComponent(short targetLayer, float knockback) {
    this.targetLayer = targetLayer;
    this.knockbackForce = knockback;
  }

  /**
   * @return knockback impulse applied to targets hit; 0 for none
   */
  public float getKnockbackForce() {
    return knockbackForce;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);
    combatStats = entity.getComponent(CombatStatsComponent.class);
    hitboxComponent = entity.getComponent(HitboxComponent.class);
  }

  /**
   * Re-deals damage on a fixed interval while contact with a target is sustained without ever
   * separating (e.g. a frozen, immobilised target that can't be knocked away).
   */
  @Override
  public void update() {
    if (overlappingTarget == null) {
      return;
    }

    GameTime time = ServiceLocator.getTimeSource();
    if (time == null) {
      return;
    }

    if (time.getTime() - lastHitTime >= REPEAT_HIT_INTERVAL_MS) {
      attack(overlappingTarget);
    }
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      // Not triggered by hitbox, ignore
      return;
    }

    if (!PhysicsLayer.contains(targetLayer, other.getFilterData().categoryBits)) {
      // Doesn't match our target layer, ignore
      return;
    }

    Entity target = ((BodyUserData) other.getBody().getUserData()).entity;
    overlappingTarget = target;
    attack(target);
  }

  private void onCollisionEnd(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me || overlappingTarget == null) {
      return;
    }

    if (!(other.getBody().getUserData() instanceof BodyUserData)) {
      return;
    }

    Entity endedWith = ((BodyUserData) other.getBody().getUserData()).entity;
    if (endedWith == overlappingTarget) {
      // Contact genuinely broke, so the next hit should come from a fresh collision again.
      overlappingTarget = null;
    }
  }

  /** Deals damage and knockback to a target we are (or still are) touching. */
  private void attack(Entity target) {
    if (PhysicsLayer.contains(targetLayer, PhysicsLayer.PLAYER)
        && StatusEffectsControllerComponent.isConcealed(target)) {
      // A hostile cannot find a concealed target, so it neither damages nor shoves them.
      return;
    }

    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (targetStats != null) {
      targetStats.hit(combatStats);
    }

    GameTime time = ServiceLocator.getTimeSource();
    if (time != null) {
      lastHitTime = time.getTime();
    }

    // Apply knockback
    PhysicsComponent physicsComponent = target.getComponent(PhysicsComponent.class);
    if (physicsComponent != null && knockbackForce > 0f) {
      Body targetBody = physicsComponent.getBody();
      Vector2 direction = target.getCenterPosition().sub(entity.getCenterPosition());
      Vector2 impulse = direction.setLength(knockbackForce);
      targetBody.applyLinearImpulse(impulse, targetBody.getWorldCenter(), true);
    }
  }
}
