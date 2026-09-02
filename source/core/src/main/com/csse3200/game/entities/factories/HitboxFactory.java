package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.weapons.FollowComponent;
import com.csse3200.game.components.weapons.LifetimeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
/**
 * Builds short-lived weapon sensor entities. The returned entity is <strong>not</strong>
 * registered; callers should {@code ServiceLocator.getEntityService().register(entity)}.
 *
 * <p>Do not instantiate this class.
 *
 * @see HitboxSpec
 * @see PhysicsLayer#WEAPON
 */
public class HitboxFactory {
  /**
   * Create an unregistered kinematic sensor that damages {@code spec.targetLayer} on contact.
   *
   * @param spec spawn configuration
   * @return unregistered hitbox entity
   * @require spec != null &amp;&amp; spec.getPosition() != null &amp;&amp; spec.getSize() != null
   *     &amp;&amp; spec.getSize().x &gt; 0 &amp;&amp; spec.getSize().y &gt; 0 &amp;&amp;
   *     spec.getLifetime() &gt;= 0 &amp;&amp; spec.getDamage() &gt;= 0 &amp;&amp;
   *     spec.getKnockback() &gt;= 0
   * @throws IllegalArgumentException if spec or its required fields are invalid
   */
  public static Entity createHitbox(HitboxSpec spec) {
    validate(spec);

    Entity hitbox =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.KinematicBody))
            .addComponent(new HitboxComponent().setLayer(spec.getLayer()))
            .addComponent(new CombatStatsComponent(0, spec.getDamage()))
            .addComponent(new TouchAttackComponent(spec.getTargetLayer(), spec.getKnockback()))
            .addComponent(new LifetimeComponent(spec.getLifetime()));

    if (spec.getOwner() != null) {
      hitbox.addComponent(new FollowComponent(spec.getOwner(), spec.getLocalOffset()));
    }
    if (spec.getTexture() != null) {
        hitbox.addComponent((new TextureRenderComponent(spec.getTexture())));
    }

    Vector2 size = spec.getSize();
    Vector2 position = spec.getPosition();
    hitbox.setScale(size);
    hitbox.setPosition(position);
    return hitbox;
  }

  private static void validate(HitboxSpec spec) {
    if (spec == null) {
      throw new IllegalArgumentException("spec must not be null");
    }
    Vector2 position = spec.getPosition();
    Vector2 size = spec.getSize();
    if (position == null) {
      throw new IllegalArgumentException("position must not be null");
    }
    if (size == null || size.x <= 0f || size.y <= 0f) {
      throw new IllegalArgumentException("size must have positive width and height");
    }
    if (spec.getLifetime() < 0f) {
      throw new IllegalArgumentException("lifetime must be >= 0");
    }
    if (spec.getDamage() < 0) {
      throw new IllegalArgumentException("damage must be >= 0");
    }
    if (spec.getKnockback() < 0f) {
      throw new IllegalArgumentException("knockback must be >= 0");
    }
  }

  private HitboxFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
