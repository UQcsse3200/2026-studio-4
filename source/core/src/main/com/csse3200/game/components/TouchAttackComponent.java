package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * When this entity touches a valid enemy's hitbox, deal damage to them and apply a knockback.
 *
 * <p>Requires CombatStatsComponent, HitboxComponent on this entity.
 *
 * <p>Damage is only applied if target entity has a CombatStatsComponent. Knockback is only applied
 * if target entity has a PhysicsComponent.
 */
public class TouchAttackComponent extends Component {
  private short targetLayer;

  /** Seconds a knocked-back target stops steering, so the impulse is not cancelled next frame. */
  public static final float KNOCKBACK_STAGGER = 0.25f;

  private float knockbackForce = 0f;
  private Entity knockbackSource;
  private CombatStatsComponent combatStats;
  private HitboxComponent hitboxComponent;
  private float repeatInterval;
  private boolean retainAttackCooldown;
  private long nextAttackTime;
  private boolean disposed;
  private final Map<Entity, Contact> contacts = new HashMap<>();

  private static class Contact {
    private final Set<Fixture> fixtures = new HashSet<>();
    private float remaining;

    private Contact(float interval) {
      remaining = interval;
    }
  }

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
   * Create a contact attack that repeats while touching a target. Existing constructors only attack
   * on contact entry, so weapon and projectile behaviour is unchanged.
   *
   * @param targetLayer target physics layers
   * @param knockback knockback impulse
   * @param repeatInterval seconds between hits; must be finite and positive
   */
  public TouchAttackComponent(short targetLayer, float knockback, float repeatInterval) {
    this(targetLayer, knockback, repeatInterval, false);
  }

  /**
   * Creates a repeating attack with an optional shared cooldown that survives contact exit.
   *
   * @param targetLayer target physics layers
   * @param knockback knockback impulse
   * @param repeatInterval seconds between hits; must be finite and positive
   * @param retainAttackCooldown whether all contacts share a cooldown, including after re-entry
   */
  public TouchAttackComponent(
      short targetLayer, float knockback, float repeatInterval, boolean retainAttackCooldown) {
    this(targetLayer, knockback);
    if (!Float.isFinite(repeatInterval) || repeatInterval <= 0f) {
      throw new IllegalArgumentException("Repeat interval must be finite and positive");
    }
    this.repeatInterval = repeatInterval;
    this.retainAttackCooldown = retainAttackCooldown;
  }

  /**
   * @return knockback impulse applied to targets hit; 0 for none
   */
  public float getKnockbackForce() {
    return knockbackForce;
  }

  /**
   * Sets the entity knockback pushes targets away from, e.g. the wielder of a weapon hitbox.
   * Without one, targets are pushed away from this entity's centre, which can pull an enemy
   * standing between a wielder and the middle of a sword swing towards the wielder.
   *
   * @param knockbackSource entity to push targets away from; null to use this entity
   * @return this component, for chaining
   */
  public TouchAttackComponent setKnockbackSource(Entity knockbackSource) {
    this.knockbackSource = knockbackSource;
    return this;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    if (repeatInterval > 0f) {
      entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);
    }
    combatStats = entity.getComponent(CombatStatsComponent.class);
    hitboxComponent = entity.getComponent(HitboxComponent.class);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (disposed || hitboxComponent.getFixture() != me) {
      // Not triggered by hitbox, ignore
      return;
    }

    if (!PhysicsLayer.contains(targetLayer, other.getFilterData().categoryBits)) {
      // Doesn't match our target layer, ignore
      return;
    }

    // Try to attack target.
    Entity target = ((BodyUserData) other.getBody().getUserData()).entity;
    if (repeatInterval > 0f) {
      Contact contact = contacts.get(target);
      if (contact != null) {
        contact.fixtures.add(other);
        return;
      }
      contact = new Contact(repeatInterval);
      contact.fixtures.add(other);
      contacts.put(target, contact);
    }
    attack(target);
  }

  private void onCollisionEnd(Fixture me, Fixture other) {
    if (disposed || hitboxComponent.getFixture() != me) {
      return;
    }
    // Do not inspect native fixture data here: a body may be being destroyed.
    contacts
        .values()
        .removeIf(
            contact -> {
              contact.fixtures.remove(other);
              return contact.fixtures.isEmpty();
            });
  }

  @Override
  public void update() {
    if (disposed || contacts.isEmpty()) {
      return;
    }
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    // A hit can synchronously cause death/disposal and change the active contacts.
    for (Map.Entry<Entity, Contact> entry : new ArrayList<>(contacts.entrySet())) {
      Contact contact = entry.getValue();
      if (contacts.get(entry.getKey()) != contact) {
        continue;
      }
      if (retainAttackCooldown) {
        attack(entry.getKey());
      } else {
        contact.remaining -= delta;
        if (contact.remaining <= 0f) {
          // No burst of catch-up hits after a long frame.
          contact.remaining = repeatInterval;
          attack(entry.getKey());
        }
      }
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    contacts.clear();
  }

  private void attack(Entity target) {
    if (retainAttackCooldown && ServiceLocator.getTimeSource().getTime() < nextAttackTime) {
      return;
    }
    if (repeatInterval > 0f
        && (combatStats.isDead() || StatusEffectsControllerComponent.isImmobilised(entity))) {
      return;
    }
    if (PhysicsLayer.contains(targetLayer, PhysicsLayer.PLAYER)
        && StatusEffectsControllerComponent.isConcealed(target)) {
      // A hostile cannot find a concealed target, so it neither damages nor shoves them.
      return;
    }
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (repeatInterval > 0f && targetStats != null && targetStats.isDead()) {
      return;
    }
    if (retainAttackCooldown) {
      nextAttackTime =
          ServiceLocator.getTimeSource().getTime() + Math.round(repeatInterval * 1000f);
    }
    entity.getEvents().trigger("touchAttack");
    if (targetStats != null) {
      targetStats.hit(combatStats);
    }

    applyKnockback(target);
  }

  /**
   * Pushes the target away from the knockback source and briefly stops it steering, so the push is
   * not cancelled by its movement on the next frame. Does nothing without knockback or physics.
   *
   * @param target entity that was hit
   */
  private void applyKnockback(Entity target) {
    PhysicsComponent physicsComponent = target.getComponent(PhysicsComponent.class);
    if (physicsComponent == null || knockbackForce <= 0f) {
      return;
    }
    Body targetBody = physicsComponent.getBody();
    Entity source = knockbackSource != null ? knockbackSource : entity;
    Vector2 direction = target.getCenterPosition().sub(source.getCenterPosition());
    if (direction.isZero()) {
      direction = target.getCenterPosition().sub(entity.getCenterPosition());
    }
    Vector2 impulse = direction.setLength(knockbackForce);
    targetBody.applyLinearImpulse(impulse, targetBody.getWorldCenter(), true);

    PhysicsMovementComponent movement = target.getComponent(PhysicsMovementComponent.class);
    if (movement != null) {
      movement.stagger(KNOCKBACK_STAGGER);
    }
    if (targetStats != null) {
      entity.getEvents().trigger("contactAttack", target);
    }
  }
}
