package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
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
  private short targetLayer;
  private float knockbackForce = 0f;
  private CombatStatsComponent combatStats;
  private HitboxComponent hitboxComponent;
  private float repeatInterval;
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
    this(targetLayer, knockback);
    if (!Float.isFinite(repeatInterval) || repeatInterval <= 0f) {
      throw new IllegalArgumentException("Repeat interval must be finite and positive");
    }
    this.repeatInterval = repeatInterval;
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
      contact.remaining -= delta;
      if (contact.remaining <= 0f) {
        // No burst of catch-up hits after a long frame.
        contact.remaining = repeatInterval;
        attack(entry.getKey());
      }
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    contacts.clear();
  }

  private void attack(Entity target) {
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
    if (targetStats != null) {
      targetStats.hit(combatStats);
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
