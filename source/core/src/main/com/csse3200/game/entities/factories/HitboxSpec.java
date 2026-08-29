package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;

/**
 * Fluent spawn configuration for {@link HitboxFactory#createHitbox(HitboxSpec)}.
 *
 * <p>Melee weapons should set {@link #owner(Entity)} so the sensor follows the wielder. Bow splash
 * should omit owner so the hitbox stays in world space.
 */
public class HitboxSpec {
  private Vector2 position;
  private Vector2 size;
  private float lifetime;
  private short layer = PhysicsLayer.WEAPON;
  private short targetLayer = PhysicsLayer.NPC;
  private int damage;
  private float knockback;
  private Entity owner;
  private Vector2 localOffset = new Vector2();

  /**
   * @param position world position of the hitbox entity
   * @return this spec
   * @require position != null
   */
  public HitboxSpec position(Vector2 position) {
    this.position = position == null ? null : position.cpy();
    return this;
  }

  /**
   * @param size width and height in metres
   * @return this spec
   * @require size != null
   */
  public HitboxSpec size(Vector2 size) {
    this.size = size == null ? null : size.cpy();
    return this;
  }

  /**
   * @param lifetime seconds until the hitbox is disposed
   * @return this spec
   */
  public HitboxSpec lifetime(float lifetime) {
    this.lifetime = lifetime;
    return this;
  }

  /**
   * Physics category of this sensor. Use {@link PhysicsLayer#WEAPON}, not {@link
   * PhysicsLayer#PLAYER}.
   *
   * @param layer physics layer bit
   * @return this spec
   */
  public HitboxSpec layer(short layer) {
    this.layer = layer;
    return this;
  }

  /**
   * Physics category this hitbox can damage.
   *
   * @param targetLayer typically {@link PhysicsLayer#NPC}
   * @return this spec
   */
  public HitboxSpec targetLayer(short targetLayer) {
    this.targetLayer = targetLayer;
    return this;
  }

  /**
   * @param damage damage applied via {@code TouchAttackComponent}
   * @return this spec
   */
  public HitboxSpec damage(int damage) {
    this.damage = damage;
    return this;
  }

  /**
   * @param knockback knockback impulse; 0 for none
   * @return this spec
   */
  public HitboxSpec knockback(float knockback) {
    this.knockback = knockback;
    return this;
  }

  /**
   * When set, the factory attaches a follow component so the hitbox tracks this entity.
   *
   * @param owner wielder to follow; null for a world-space splash
   * @return this spec
   */
  public HitboxSpec owner(Entity owner) {
    this.owner = owner;
    return this;
  }

  /**
   * World-axis offset from {@link #owner(Entity)}. Ignored when owner is null.
   *
   * @param localOffset offset from the owner position
   * @return this spec
   */
  public HitboxSpec localOffset(Vector2 localOffset) {
    this.localOffset = localOffset == null ? new Vector2() : localOffset.cpy();
    return this;
  }

  /**
   * @return copy of world position, or null if unset
   */
  public Vector2 getPosition() {
    return position == null ? null : position.cpy();
  }

  /**
   * @return copy of size in metres, or null if unset
   */
  public Vector2 getSize() {
    return size == null ? null : size.cpy();
  }

  /**
   * @return lifetime in seconds
   */
  public float getLifetime() {
    return lifetime;
  }

  /**
   * @return physics category of this sensor
   */
  public short getLayer() {
    return layer;
  }

  /**
   * @return physics category this hitbox can damage
   */
  public short getTargetLayer() {
    return targetLayer;
  }

  /**
   * @return damage applied on contact
   */
  public int getDamage() {
    return damage;
  }

  /**
   * @return knockback impulse magnitude
   */
  public float getKnockback() {
    return knockback;
  }

  /**
   * @return wielder to follow, or null for world-space splash
   */
  public Entity getOwner() {
    return owner;
  }

  /**
   * @return copy of the follow offset
   */
  public Vector2 getLocalOffset() {
    return localOffset.cpy();
  }
}
